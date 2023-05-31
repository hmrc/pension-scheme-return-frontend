/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import controllers.nonsipp.memberdetails.{MemberDetailsController, MemberDetailsNinoController, NoNINOController}
import forms.{NameDOBFormProvider, TextFormProvider}
import models._
import pages.nonsipp.memberdetails._
import play.api.data.Form
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Integral.Implicits.infixIntegralOps
import scala.util.Try

class MemberDetailsUploadValidator @Inject()(
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider
)(implicit ec: ExecutionContext) {

  private val memberDetailsForm = MemberDetailsController.form(nameDOBFormProvider)
  private def ninoForm(memberDetails: NameDOB, previousNinos: List[Nino]) =
    MemberDetailsNinoController.form(textFormProvider, memberDetails, previousNinos)

  private def noNinoForm(memberDetails: NameDOB) =
    NoNINOController.form(textFormProvider, memberDetails.fullName)

  private val firstRowSink: Sink[ByteString, Future[String]] =
    Sink.head[ByteString].mapMaterializedValue(_.map(_.utf8String))

  private val csvFrame: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString("\r\n"), maximumFrameLength = Int.MaxValue, allowTruncation = true)

  private val aToZ: List[Char] = ('a' to 'z').toList.map(_.toUpper)

  def validateCSV(
    source: Source[ByteString, _]
  )(implicit mat: Materializer): Future[Upload] = {
    val csvFrames = source.via(csvFrame)
    for {
      csvHeader <- csvFrames.runWith(firstRowSink)
      header = csvHeader
        .split(",")
        .zipWithIndex
        .map { case (key, index) => CsvHeaderKey(key, indexToCsvKey(index), index) }
        .toList
      validated <- csvFrames
        .drop(1) // drop csv header and process rows
        .statefulMap[UploadState, Upload](() => UploadState.init)(
          (state, bs) => {
            val parts = bs.utf8String.split(",").toList
            validateMemberDetails(header, parts, state.previousNinos, state.row) match {
              case None => state.next() -> UploadFormatError
              case Some(Valid(memberDetails)) =>
                state.next(memberDetails.ninoOrNoNinoReason.toOption) -> UploadSuccess(List(memberDetails))
              case Some(Invalid(errs)) => state.next() -> UploadErrors(errs)
            }
          },
          _ => None
        )
        .takeWhile({
          case UploadFormatError => false
          case _ => true
        }, inclusive = true)
        .runReduce[Upload] {
          // format error
          case (_, UploadFormatError) => UploadFormatError
          case (UploadFormatError, _) => UploadFormatError
          // errors
          case (UploadErrors(previous), UploadErrors(errs)) => UploadErrors(previous ++ errs)
          case (errs: UploadErrors, _) => errs
          case (_, errs: UploadErrors) => errs
          // success
          case (previous: UploadSuccess, current: UploadSuccess) =>
            UploadSuccess(previous.memberDetails ++ current.memberDetails)
          case (_, memberDetails: UploadSuccess) => memberDetails
        }
    } yield validated
  }

  private def validateMemberDetails(
    headerKeys: List[CsvHeaderKey],
    csvData: List[String],
    previousNinos: List[Nino],
    row: Int
  ): Option[Validated[List[ValidationError], UploadMemberDetails]] = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

    for {
      firstName <- getCSVValue(UploadKeys.firstName, headerKeys, csvData)
      lastName <- getCSVValue(UploadKeys.lastName, headerKeys, csvData)
      dob <- getCSVValue(UploadKeys.dateOfBirth, headerKeys, csvData)
      parsedDOB <- Try(LocalDate.parse(dob.value, dateTimeFormatter)).toOption
        .map(localDate => CsvValue(dob.key, localDate))
      maybeNino <- getOptionalCSVValue(UploadKeys.nino, headerKeys, csvData)
      maybeNoNinoReason <- getOptionalCSVValue(UploadKeys.reasonForNoNino, headerKeys, csvData)
      validatedNameDOB = validateNameDOB(firstName, lastName, parsedDOB, row)
      nameDob = NameDOB(firstName.value, lastName.value, parsedDOB.value)
      maybeValidatedNino = maybeNino.value.map(
        nino => validateNino(maybeNino.as(nino), nameDob, previousNinos, row)
      )
      maybeValidatedNoNinoReason = maybeNoNinoReason.value.map(
        reason => validateNoNino(maybeNoNinoReason.as(reason), nameDob, row)
      )
      validatedNinoOrNoNinoReason <- (maybeValidatedNino, maybeValidatedNoNinoReason) match {
        case (Some(validatedNino), None) => Some(Right(validatedNino))
        case (None, Some(validatedNoNinoReason)) => Some(Left(validatedNoNinoReason))
        case (_, _) => None // fail if neither or both are present in csv
      }
    } yield (
      validatedNameDOB,
      validatedNinoOrNoNinoReason.bisequence
    ).mapN((nameDob, ninoOrNoNinoReason) => UploadMemberDetails(row, nameDob, ninoOrNoNinoReason))
  }

  private def validateNameDOB(
    firstName: CsvValue[String],
    lastName: CsvValue[String],
    dob: CsvValue[LocalDate],
    row: Int
  ): Validated[List[ValidationError], NameDOB] = {
    val dobDayKey = s"${nameDOBFormProvider.dateOfBirth}.day"
    val dobMonthKey = s"${nameDOBFormProvider.dateOfBirth}.month"
    val dobYearKey = s"${nameDOBFormProvider.dateOfBirth}.year"

    val boundForm = memberDetailsForm
      .bind(
        Map(
          nameDOBFormProvider.firstName -> firstName.value,
          nameDOBFormProvider.lastName -> lastName.value,
          dobDayKey -> dob.value.getDayOfMonth.toString,
          dobMonthKey -> dob.value.getMonthValue.toString,
          dobYearKey -> dob.value.getYear.toString
        )
      )

    val keys = List(
      nameDOBFormProvider.firstName -> firstName.key,
      nameDOBFormProvider.lastName -> lastName.key,
      nameDOBFormProvider.dateOfBirth -> dob.key
    )

    formToResult(boundForm, keys, row)
  }

  private def validateNino(
    nino: CsvValue[String],
    nameDob: NameDOB,
    previousNinos: List[Nino],
    row: Int
  ): Validated[List[ValidationError], Nino] = {
    val boundForm = ninoForm(nameDob, previousNinos)
      .bind(
        Map(
          textFormProvider.formKey -> nino.value
        )
      )

    val keys = List(
      textFormProvider.formKey -> nino.key
    )

    formToResult(boundForm, keys, row)
  }

  private def validateNoNino(
    noNinoReason: CsvValue[String],
    nameDob: NameDOB,
    row: Int
  ): Validated[List[ValidationError], String] = {
    val boundForm = noNinoForm(nameDob)
      .bind(
        Map(
          textFormProvider.formKey -> noNinoReason.value
        )
      )

    val keys = List(
      textFormProvider.formKey -> noNinoReason.key
    )

    formToResult(boundForm, keys, row)
  }

  private def getCSVValue(
    key: String,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[CsvValue[String]] =
    getOptionalCSVValue(key, headerKeys, csvData) match {
      case Some(CsvValue(key, Some(value))) => Some(CsvValue(key, value))
      case _ => None
    }

  private def getOptionalCSVValue(
    key: String,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[CsvValue[Option[String]]] =
    headerKeys
      .find(_.key.toLowerCase() == key.toLowerCase())
      .map(foundKey => CsvValue(foundKey, csvData.get(foundKey.index).flatMap(s => if (s.isEmpty) None else Some(s))))

  private def formToResult[A](
    form: Form[A],
    keys: List[(String, CsvHeaderKey)],
    row: Int
  ): Validated[List[ValidationError], A] =
    form.fold(
      hasErrors = _.errors
        .flatMap { err =>
          keys
            .find { case (formKey, _) => formKey == err.key }
            .map {
              case (_, hk) =>
                ValidationError(hk.cell + row, err.message)
            }
            .toList
        }
        .toList
        .invalid,
      success = _.valid
    )

  private def indexToCsvKey(index: Int): String =
    if (index == 0) aToZ.head.toString
    else {
      val (quotient, remainder) = index /% (aToZ.size)
      if (quotient == 0) aToZ(remainder).toString
      else indexToCsvKey(quotient - 1) + indexToCsvKey(remainder)
    }
}
