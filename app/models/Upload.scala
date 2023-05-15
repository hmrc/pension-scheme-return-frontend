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

import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import utils.ListUtils.ListOps

case class ValidationError(key: String, message: String)

object ValidationError {
  implicit val format: Format[ValidationError] = Json.format[ValidationError]
}

case class UploadState(row: Int, previousNinos: List[Nino]) {
  def next(nino: Option[Nino] = None): UploadState =
    UploadState(row + 1, previousNinos :?+ nino)
}

object UploadState {
  val init: UploadState = UploadState(1, Nil)
}

sealed trait Upload

case object UploadInitial extends UploadFailure

case class UploadSuccess(memberDetails: UploadMemberDetails) extends Upload

sealed trait UploadFailure extends Upload

object UploadFailure {
  implicit val writes: Writes[UploadFailure] = {
    case UploadErrors(errs) => Json.obj("_type" -> "UploadErrors", "errors" -> Json.toJson(errs))
    case UploadFormatError => Json.obj("_type" -> "UploadFormatError")
  }

  implicit val read: Reads[UploadFailure] =
    (__ \ "_type").read[String].flatMap {
      case "UploadFormatError" => Reads.pure(UploadFormatError)
      case "UploadErrors" => (__ \ "errors").read[List[ValidationError]].map(UploadErrors)
    }
}

object UploadFormatError extends UploadFailure

case class UploadErrors(errors: List[ValidationError]) extends UploadFailure

case class UploadMemberDetails(row: Int, nameDOB: NameDOB, ninoOrNoNinoReason: Either[String, Nino])

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
