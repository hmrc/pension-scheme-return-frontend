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

package repositories

import models._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.Function.unlift
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UploadDetails](
      collectionName = "upload",
      mongoComponent = mongoComponent,
      domainFormat = UploadRepository.mongoFormat,
      indexes = Seq(
        IndexModel(Indexes.ascending("id"), IndexOptions().unique(true)),
        IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true))
      ),
      replaceIndexes = true
    ) {

  import UploadRepository._

  def insert(details: UploadDetails): Future[Unit] =
    collection
      .insertOne(details)
      .toFuture()
      .map(_ => ())

  def find(key: UploadKey): Future[Option[UploadDetails]] =
    collection.find(equal("id", Codecs.toBson(key))).headOption()

  def updateStatus(reference: Reference, newStatus: UploadStatus): Future[UploadStatus] =
    collection
      .findOneAndUpdate(
        filter = equal("reference", Codecs.toBson(reference)),
        update = set("status", Codecs.toBson(newStatus)),
        options = FindOneAndUpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_.status)

  def remove(key: UploadKey): Future[Unit] =
    collection
      .deleteOne(equal("id", Codecs.toBson(key.value)))
      .toFuture()
      .map(_ => ())
}

object UploadRepository {
  val status = "status"

  private implicit val uploadStatusFormat: Format[UploadStatus] = {
    implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]
    val read: Reads[UploadStatus] = (json: JsValue) => {
      val jsObject = json.asInstanceOf[JsObject]
      jsObject.value.get("_type") match {
        case Some(JsString("InProgress")) => JsSuccess(InProgress)
        case Some(JsString("Failed")) => JsSuccess(Failed)
        case Some(JsString("UploadedSuccessfully")) =>
          Json.fromJson[UploadedSuccessfully](jsObject)(uploadedSuccessfullyFormat)
        case Some(value) => JsError(s"Unexpected value of _type: $value")
        case None => JsError("Missing _type field")
      }
    }

    val write: Writes[UploadStatus] = {
      case InProgress => JsObject(Map("_type" -> JsString("InProgress")))
      case Failed => JsObject(Map("_type" -> JsString("Failed")))
      case s: UploadedSuccessfully =>
        Json.toJson(s)(uploadedSuccessfullyFormat).as[JsObject] + ("_type" -> JsString("UploadedSuccessfully"))
    }

    Format(read, write)
  }

  private implicit val referenceFormat: Format[Reference] =
    stringFormat[Reference](Reference(_), _.reference)

  private[repositories] val mongoFormat: OFormat[UploadDetails] = {
    (
      (__ \ "id").format[UploadKey] ~
        (__ \ "reference").format[Reference] ~
        (__ \ "status").format[UploadStatus]
    )(UploadDetails.apply, unlift(UploadDetails.unapply))
  }

  private def stringFormat[A](to: String => A, from: A => String): Format[A] =
    Format[A](Reads.StringReads.map(to), Writes.StringWrites.contramap(from))
}
