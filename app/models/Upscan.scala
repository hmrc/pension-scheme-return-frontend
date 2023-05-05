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

import models.SchemeId.{asSrn, Srn}
import models.requests.DataRequest
import org.bson.types.ObjectId
import play.api.libs.json.{
  Format,
  JsDefined,
  JsError,
  JsString,
  JsUndefined,
  JsValue,
  Json,
  OFormat,
  OWrites,
  Reads,
  Writes
}
import play.api.mvc.QueryStringBindable
import utils.HttpUrlFormat

import java.net.URL
import java.time.{Instant, LocalDateTime}
import java.util.UUID

case class Reference(reference: String)

object Reference {
  implicit val referenceReader: Reads[Reference] = Reads.StringReads.map(Reference(_))
  implicit val referenceWrites: Writes[Reference] = Writes.StringWrites.contramap(_.reference)
}

case class UploadForm(href: String, fields: Map[String, String])

case class PreparedUpload(reference: Reference, uploadRequest: UploadForm)

object PreparedUpload {

  implicit val uploadFormFormat: Format[UploadForm] = Json.format[UploadForm]

  implicit val format: Format[PreparedUpload] = Json.format[PreparedUpload]
}

case class UpscanInitiateRequest(
  callbackUrl: String,
  successRedirect: Option[String] = None,
  errorRedirect: Option[String] = None,
  minimumFileSize: Option[Int] = None,
  maximumFileSize: Option[Int] = None
)

object UpscanInitiateRequest {
  implicit val format: OFormat[UpscanInitiateRequest] = Json.format[UpscanInitiateRequest]
}

case class UploadKey private (userId: String, srn: Srn) {
  val value: String = userId + UploadKey.separator + srn.value
}

object UploadKey {
  def fromRequest(srn: Srn)(implicit req: DataRequest[_]): UploadKey =
    UploadKey(req.getUserId, srn)

  val separator = "&&"

  implicit val reads: Reads[UploadKey] = Reads.StringReads.flatMap(_.split(separator).toList match {
    case List(userId, asSrn(srn)) => Reads.pure(UploadKey(userId, srn))
    case key => Reads.failed(s"Upload key $key is in wrong format. It should be userId${separator}srn")
  })

  implicit val writes: Writes[UploadKey] = Writes.StringWrites.contramap(_.value)
}

case class UpscanInitiateError(e: Throwable) extends RuntimeException(e)

case class UpscanFileReference(reference: String)

case class UpscanInitiateResponse(
  fileReference: UpscanFileReference,
  postTarget: String,
  formFields: Map[String, String]
)

object UpscanInitiateResponse {
  implicit val refFormat: OFormat[UpscanFileReference] = Json.format[UpscanFileReference]
  implicit val format: OFormat[UpscanInitiateResponse] = Json.format[UpscanInitiateResponse]
}

sealed trait UploadStatus

case object InProgress extends UploadStatus

case object Failed extends UploadStatus

case class UploadedSuccessfully(name: String, mimeType: String, downloadUrl: String, size: Option[Long])
    extends UploadStatus

object UploadedSuccessfully {
  implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]
}

case class FileUploadStatus(
  _type: String,
  failureReason: Option[String] = None,
  message: Option[String] = None,
  downloadUrl: Option[String] = None,
  mimeType: Option[String] = None,
  name: Option[String] = None,
  size: Option[Long] = None
)

object FileUploadStatus {
  implicit val reads: OFormat[FileUploadStatus] = Json.format[FileUploadStatus]
}

case class FileUploadDataCache(
  uploadId: String,
  reference: String,
  status: FileUploadStatus,
  created: LocalDateTime,
  lastUpdated: LocalDateTime,
  expireAt: LocalDateTime
)

object FileUploadDataCache {
  implicit val reads: OFormat[FileUploadDataCache] = Json.format[FileUploadDataCache]
}

case class UploadId(value: String) extends AnyVal

object UploadId {
  def generate: UploadId = UploadId(UUID.randomUUID().toString)

  implicit def queryBinder(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[UploadId] =
    stringBinder.transform(UploadId(_), _.value)
}

sealed trait CallbackBody {
  def reference: Reference
}

case class ReadyCallbackBody(
  reference: Reference,
  downloadUrl: URL,
  uploadDetails: UploadCallbackDetails
) extends CallbackBody

case class FailedCallbackBody(
  reference: Reference,
  failureDetails: ErrorDetails
) extends CallbackBody

case class UploadCallbackDetails(
  uploadTimestamp: Instant,
  checksum: String,
  fileMimeType: String,
  fileName: String,
  size: Long
)

case class UploadDetails(key: UploadKey, reference: Reference, status: UploadStatus)

case class ErrorDetails(failureReason: String, message: String)

object CallbackBody {
  // must be in scope to create Reads for ReadyCallbackBody
  private implicit val urlFormat: Format[URL] = HttpUrlFormat.format

  implicit val uploadDetailsReads: Reads[UploadCallbackDetails] = Json.reads[UploadCallbackDetails]

  implicit val errorDetailsReads: Reads[ErrorDetails] = Json.reads[ErrorDetails]

  implicit val readyCallbackBodyReads: Reads[ReadyCallbackBody] = Json.reads[ReadyCallbackBody]

  implicit val failedCallbackBodyReads: Reads[FailedCallbackBody] = Json.reads[FailedCallbackBody]

  implicit val reads: Reads[CallbackBody] = (json: JsValue) =>
    json \ "fileStatus" match {
      case JsDefined(JsString("READY")) => implicitly[Reads[ReadyCallbackBody]].reads(json)
      case JsDefined(JsString("FAILED")) => implicitly[Reads[FailedCallbackBody]].reads(json)
      case JsDefined(value) => JsError(s"Invalid type distriminator: $value")
      case JsUndefined() => JsError(s"Missing type distriminator")
      case _ => JsError(s"Missing type distriminator")
    }
}
