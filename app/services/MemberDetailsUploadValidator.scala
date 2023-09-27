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
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._
import config.Constants
import controllers.nonsipp.memberdetails.{MemberDetailsController, MemberDetailsNinoController, NoNINOController}
import forms.{NameDOBFormProvider, TextFormProvider}
import models.SchemeId.Srn
import models.ValidationErrorType.ValidationErrorType
import models._
import models.requests.DataRequest
import play.api.data.{Form, FormError}
import play.api.i18n.I18nSupport.RequestWithMessagesApi
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Integral.Implicits.infixIntegralOps

class MemberDetailsUploadValidator @Inject()(
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext) {

  private def ninoForm(memberFullName: String, previousNinos: List[Nino]): Form[Nino] =
    MemberDetailsNinoController.form(textFormProvider, memberFullName, previousNinos)

  private def noNinoForm(memberFullName: String): Form[String] =
    NoNINOController.uploadFormSummaryErrors(textFormProvider, memberFullName)

  private val firstRowSink: Sink[List[ByteString], Future[List[String]]] =
    Sink.head[List[ByteString]].mapMaterializedValue(_.map(_.map(_.utf8String)))

  private val csvFrame: Flow[ByteString, List[ByteString], NotUsed] = {
    CsvParsing.lineScanner()
  }

  private val aToZ: List[Char] = ('a' to 'z').toList.map(_.toUpper)

  def getTaxDates(srn: Srn)(implicit request: DataRequest[AnyContent]): Option[LocalDate] =
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(taxPeriod) =>
        taxPeriod.fold(l => Some(l.to), r => Some(r.map(x => x._1).toList.sortBy(_.to).reverse.head.to))
      case _ => None
    }

  def validateCSV(
    source: Source[ByteString, _],
    srn: Srn,
    request: DataRequest[AnyContent]
  )(implicit mat: Materializer, messages: Messages): Future[Upload] = {
    val csvFrames = source.via(csvFrame)
    (for {
      csvHeader <- csvFrames.runWith(firstRowSink)
      header = csvHeader.zipWithIndex
        .map { case (key, index) => CsvHeaderKey(key, indexToCsvKey(index), index) }
      validated <- csvFrames
        .drop(1) // drop csv header and process rows
        .statefulMap[UploadState, Upload](() => UploadState.init)(
          (state, bs) => {
            if (state.row > Constants.maxSchemeMembers) {
              state.next() -> UploadMaxRowsError
            } else {
              val parts = bs.map(_.utf8String)
              validateMemberDetails(header, parts, state.previousNinos, state.row, srn, request) match {
                case None => state.next() -> UploadFormatError
                case Some(Valid(memberDetails)) =>
                  state.next(memberDetails.ninoOrNoNinoReason.toOption) -> UploadSuccess(List(memberDetails))
                case Some(Invalid(errs)) => state.next() -> UploadErrors(errs)
              }
            }
          },
          _ => None
        )
        .takeWhile({
          case UploadFormatError | UploadMaxRowsError => false
          case _ => true
        }, inclusive = true)
        .runReduce[Upload] {
          // format and max row errors
          case (_, UploadFormatError) => UploadFormatError
          case (_, UploadMaxRowsError) => UploadMaxRowsError
          case (UploadFormatError, _) => UploadFormatError
          case (UploadMaxRowsError, _) => UploadMaxRowsError
          // errors
          case (UploadErrors(previous), UploadErrors(errs)) => UploadErrors(previous ++ errs.toList)
          case (errs: UploadErrors, _) => errs
          case (_, errs: UploadErrors) => errs
          // success
          case (previous: UploadSuccess, current: UploadSuccess) =>
            UploadSuccess(previous.memberDetails ++ current.memberDetails)
          case (_, memberDetails: UploadSuccess) => memberDetails
        }
    } yield validated)
      .recover {
        case _: NoSuchElementException => UploadFormatError
      }
  }

  private def validateMemberDetails(
    headerKeys: List[CsvHeaderKey],
    csvData: List[String],
    previousNinos: List[Nino],
    row: Int,
    srn: Srn,
    request: DataRequest[AnyContent]
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, UploadMemberDetails]] =
    for {
      firstName <- getCSVValue(UploadKeys.firstName, headerKeys, csvData)
      lastName <- getCSVValue(UploadKeys.lastName, headerKeys, csvData)
      dob <- getCSVValue(UploadKeys.dateOfBirth, headerKeys, csvData)
      maybeNino <- getOptionalCSVValue(UploadKeys.nino, headerKeys, csvData)
      maybeNoNinoReason <- getOptionalCSVValue(UploadKeys.reasonForNoNino, headerKeys, csvData)
      validatedNameDOB <- validateNameDOB(firstName, lastName, dob, row, srn, request)
      memberFullName = s"${firstName.value} ${lastName.value}"
      maybeValidatedNino = maybeNino.value.flatMap { nino =>
        validateNino(maybeNino.as(nino), memberFullName, previousNinos, row)
      }
      maybeValidatedNoNinoReason = maybeNoNinoReason.value.flatMap(
        reason => validateNoNino(maybeNoNinoReason.as(reason), memberFullName, row)
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

  private def validateNameDOB(
    firstName: CsvValue[String],
    lastName: CsvValue[String],
    dob: CsvValue[String],
    row: Int,
    srn: Srn,
    request: DataRequest[AnyContent]
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, NameDOB]] = {
    val dobDayKey = s"${nameDOBFormProvider.dateOfBirth}.day"
    val dobMonthKey = s"${nameDOBFormProvider.dateOfBirth}.month"
    val dobYearKey = s"${nameDOBFormProvider.dateOfBirth}.year"

    dob.value.split("/").toList match {
      case day :: month :: year :: Nil =>
        val memberDetailsForm = MemberDetailsController
          .form(nameDOBFormProvider, getTaxDates(srn)(request))
          .bind(
            Map(
              nameDOBFormProvider.firstName -> firstName.value,
              nameDOBFormProvider.lastName -> lastName.value,
              dobDayKey -> day,
              dobMonthKey -> month,
              dobYearKey -> year
            )
          )

        val errorTypeMapping: FormError => ValidationErrorType = _.key match {
          case nameDOBFormProvider.firstName => ValidationErrorType.FirstName
          case nameDOBFormProvider.lastName => ValidationErrorType.LastName
          case nameDOBFormProvider.dateOfBirth => ValidationErrorType.DateOfBirth
          case `dobDayKey` => ValidationErrorType.DateOfBirth
          case `dobMonthKey` => ValidationErrorType.DateOfBirth
          case `dobYearKey` => ValidationErrorType.DateOfBirth
        }

        val cellMapping: FormError => Option[String] = {
          case err if err.key == nameDOBFormProvider.firstName => Some(firstName.key.cell)
          case err if err.key == nameDOBFormProvider.lastName => Some(lastName.key.cell)
          case err if err.key == nameDOBFormProvider.dateOfBirth => Some(dob.key.cell)
          case err if err.key == dobDayKey => Some(dob.key.cell)
          case err if err.key == dobMonthKey => Some(dob.key.cell)
          case err if err.key == dobYearKey => Some(dob.key.cell)
          case _ => None
        }

        formToResult(memberDetailsForm, row, errorTypeMapping, cellMapping)
      case _ =>
        Some(
          ValidationError
            .fromCell(
              dob.key.cell,
              row,
              ValidationErrorType.DateOfBirth,
              messages("memberDetails.dateOfBirth.error.format")
            )
            .invalidNel
        )
    }
  }

  private def validateNino(
    nino: CsvValue[String],
    memberFullName: String,
    previousNinos: List[Nino],
    row: Int
  ): Option[ValidatedNel[ValidationError, Nino]] = {
    val boundForm = ninoForm(memberFullName, previousNinos)
      .bind(
        Map(
          textFormProvider.formKey -> nino.value
        )
      )

    val errorTypeMapping: FormError => ValidationErrorType = _.message match {
      case "memberDetailsNino.error.duplicate" => ValidationErrorType.DuplicateNino
      case _ => ValidationErrorType.NinoFormat
    }

    formToResult(boundForm, row, errorTypeMapping, cellMapping = _ => Some(nino.key.cell))
  }

  private def validateNoNino(
    noNinoReason: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = noNinoForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> noNinoReason.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.NoNinoReason,
      cellMapping = _ => Some(noNinoReason.key.cell)
    )
  }

  // Replace missing csv value with blank string so form validation can return a `value required` instead of returning a format error
  private def getCSVValue(
    key: String,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[CsvValue[String]] =
    getOptionalCSVValue(key, headerKeys, csvData) match {
      case Some(CsvValue(key, Some(value))) => Some(CsvValue(key, value))
      case Some(CsvValue(key, None)) => Some(CsvValue(key, ""))
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
    row: Int,
    errorTypeMapping: FormError => ValidationErrorType,
    cellMapping: FormError => Option[String]
  ): Option[Validated[NonEmptyList[ValidationError], A]] =
    form.fold(
      // unchecked is used as there will always be form errors here and theres no need to exhaustively pattern match and throw an unreachable exception
      hasErrors = form =>
        (form.errors: @unchecked) match {
          case head :: rest =>
            NonEmptyList
              .of[FormError](head, rest: _*)
              .map(
                err =>
                  cellMapping(err).map(cell => ValidationError.fromCell(cell, row, errorTypeMapping(err), err.message))
              )
              .sequence
              .map(_.invalid)
        },
      success = _.valid.some
    )

  private def indexToCsvKey(index: Int): String =
    if (index == 0) aToZ.head.toString
    else {
      val (quotient, remainder) = index /% (aToZ.size)
      if (quotient == 0) aToZ(remainder).toString
      else indexToCsvKey(quotient - 1) + indexToCsvKey(remainder)
    }
}
