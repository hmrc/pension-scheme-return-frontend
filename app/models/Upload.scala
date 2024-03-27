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

package models

import utils.ListUtils.ListOps
import cats.data.NonEmptyList
import models.ValidationErrorType.ValidationErrorType
import play.api.libs.functional.syntax._
import uk.gov.hmrc.domain.Nino
import play.api.libs.json._

case class ValidationError(key: String, errorType: ValidationErrorType, message: String)

object ValidationErrorType {

  sealed trait ValidationErrorType
  case object FirstName extends ValidationErrorType
  case object LastName extends ValidationErrorType
  case object DateOfBirth extends ValidationErrorType
  case object NinoFormat extends ValidationErrorType
  case object DuplicateNino extends ValidationErrorType
  case object NoNinoReason extends ValidationErrorType
}

object ValidationError {
  import ValidationErrorType._

  implicit val firstNameFormat: Format[ValidationErrorType.FirstName.type] =
    Json.format[ValidationErrorType.FirstName.type]
  implicit val lastNameFormat: Format[ValidationErrorType.LastName.type] =
    Json.format[ValidationErrorType.LastName.type]
  implicit val dobFormat: Format[ValidationErrorType.DateOfBirth.type] =
    Json.format[ValidationErrorType.DateOfBirth.type]
  implicit val ninoFormat: Format[ValidationErrorType.NinoFormat.type] =
    Json.format[ValidationErrorType.NinoFormat.type]
  implicit val duplicateNinoFormat: Format[ValidationErrorType.DuplicateNino.type] =
    Json.format[ValidationErrorType.DuplicateNino.type]
  implicit val noNinoReasonFormat: Format[ValidationErrorType.NoNinoReason.type] =
    Json.format[ValidationErrorType.NoNinoReason.type]
  implicit val errorTypeFormat: Format[ValidationErrorType] = Json.format[ValidationErrorType]
  implicit val format: Format[ValidationError] = Json.format[ValidationError]

  def fromCell(cell: String, row: Int, errorType: ValidationErrorType, errorMessage: String): ValidationError =
    ValidationError(cell + row, errorType: ValidationErrorType, errorMessage)
}

case class UploadState(row: Int, previousNinos: List[Nino]) {
  def next(nino: Option[Nino] = None): UploadState =
    UploadState(row + 1, previousNinos :?+ nino)
}

object UploadState {
  val init: UploadState = UploadState(1, Nil)
}

sealed trait Upload

case class UploadSuccess(memberDetails: List[UploadMemberDetails]) extends Upload

// UploadError should not extend Upload as the nested inheritance causes issues with the play Json macros
sealed trait UploadError

case object UploadFormatError extends Upload with UploadError

case object UploadMaxRowsError extends Upload with UploadError

case class UploadErrors(errors: NonEmptyList[ValidationError]) extends Upload with UploadError

case class UploadMemberDetails(row: Int, nameDOB: NameDOB, ninoOrNoNinoReason: Either[String, Nino])

object UploadMemberDetails {
  implicit val eitherWrites: Writes[Either[String, Nino]] = e =>
    Json.obj(
      e.fold(
        noNinoReason => "noNinoReason" -> Json.toJson(noNinoReason),
        nino => "nino" -> Json.toJson(nino)
      )
    )

  implicit val eitherReads: Reads[Either[String, Nino]] =
    (__ \ "noNinoReason").read[String].map(noNinoReason => Left(noNinoReason)) |
      (__ \ "nino").read[Nino].map(nino => Right(nino))

  implicit val format: Format[UploadMemberDetails] = Json.format[UploadMemberDetails]
}

/**
 * @param key = csv header key e.g. First name
 * @param cell = letter identifying column e.g A,B,C ... BA,BB ...
 * @param index = column number
 */
case class CsvHeaderKey(key: String, cell: String, index: Int)

case class CsvValue[A](key: CsvHeaderKey, value: A) {
  def map[B](f: A => B): CsvValue[B] = CsvValue[B](key, f(value))

  def as[B](b: B): CsvValue[B] = map(_ => b)
}
