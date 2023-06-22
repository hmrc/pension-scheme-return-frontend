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

import cats.data.NonEmptyList
import config.{Crypto, FrontendAppConfig}
import models.SchemeId.asSrn
import models.UploadKey.separator
import models.UploadStatus.UploadStatus
import models._
import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import repositories.UploadRepository.MongoUpload
import repositories.UploadRepository.MongoUpload.SensitiveUpload
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs._
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.Function.unlift
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadRepository @Inject()(
  mongoComponent: MongoComponent,
  clock: Clock,
  appConfig: FrontendAppConfig,
  crypto: Crypto
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[MongoUpload](
      collectionName = "upload",
      mongoComponent = mongoComponent,
      domainFormat = MongoUpload.format(crypto.getCrypto),
      indexes = Seq(
        IndexModel(Indexes.ascending("id"), IndexOptions().unique(true)),
        IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true)),
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.uploadTtl, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = false
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto

  import UploadRepository._

  def insert(details: UploadDetails): Future[Unit] =
    collection
      .insertOne(toMongoUpload(details))
      .toFuture()
      .map(_ => ())

  def getUploadDetails(key: UploadKey): Future[Option[UploadDetails]] =
    collection.find(equal("id", key.toBson())).headOption().map(_.map(toUploadDetails))

  def updateStatus(reference: Reference, newStatus: UploadStatus): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("reference", reference.toBson()),
        update = combine(
          set("status", newStatus.toBson()),
          set("lastUpdated", Instant.now(clock).toBson())
        ),
        options = FindOneAndUpdateOptions().upsert(false)
      )
      .toFuture()
      .map(_ => ())

  def setUploadResult(key: UploadKey, result: Upload): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("id", key.toBson()),
        update = combine(
          set("result", SensitiveUpload(result).toBson()),
          set("lastUpdated", Instant.now(clock).toBson())
        )
      )
      .toFuture()
      .map(_ => ())

  def getUploadResult(key: UploadKey): Future[Option[Upload]] =
    collection
      .find(equal("id", key.value.toBson()))
      .headOption()
      .map(_.flatMap(_.result.map(_.decryptedValue)))

  def remove(key: UploadKey): Future[Unit] =
    collection
      .deleteOne(equal("id", key.toBson()))
      .toFuture()
      .map(_ => ())

  private def toMongoUpload(details: UploadDetails): MongoUpload = MongoUpload(
    details.key,
    details.reference,
    details.status,
    details.lastUpdated,
    None
  )

  private def toUploadDetails(mongoUpload: MongoUpload): UploadDetails = UploadDetails(
    mongoUpload.key,
    mongoUpload.reference,
    mongoUpload.status,
    mongoUpload.lastUpdated
  )
}

object UploadRepository {

  case class MongoUpload(
    key: UploadKey,
    reference: Reference,
    status: UploadStatus,
    lastUpdated: Instant,
    result: Option[SensitiveUpload]
  )

  object MongoUpload {

    case class SensitiveUpload(override val decryptedValue: Upload) extends Sensitive[Upload]

    implicit def sensitiveObjectFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveUpload] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUpload.apply)
    def reads(implicit crypto: Encrypter with Decrypter): Reads[MongoUpload] =
      (__ \ "id")
        .read[UploadKey]
        .and((__ \ "reference").read[Reference])
        .and((__ \ "status").read[UploadStatus])
        .and((__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat))
        .and((__ \ "result").readNullable[SensitiveUpload])(MongoUpload.apply _)

    def writes(implicit crypto: Encrypter with Decrypter): OWrites[MongoUpload] =
      (__ \ "id")
        .write[UploadKey]
        .and((__ \ "reference").write[Reference])
        .and((__ \ "status").write[UploadStatus])
        .and((__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat))
        .and((__ \ "result").writeNullable[SensitiveUpload])(
          unlift(MongoUpload.unapply)
        )

    implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[MongoUpload] = OFormat(reads, writes)
  }

  implicit val uploadKeyReads: Reads[UploadKey] = Reads.StringReads.flatMap(_.split(separator).toList match {
    case List(userId, asSrn(srn)) => Reads.pure(UploadKey(userId, srn))
    case key => Reads.failed(s"Upload key $key is in wrong format. It should be userId${separator}srn")
  })

  implicit val uploadKeyWrites: Writes[UploadKey] = Writes.StringWrites.contramap(_.value)

  implicit val uploadedSuccessfullyFormat: OFormat[UploadStatus.Success] =
    Json.format[UploadStatus.Success]
  implicit val uploadedFailedFormat: OFormat[UploadStatus.Failed.type] = Json.format[UploadStatus.Failed.type]
  implicit val uploadedInProgressFormat: OFormat[UploadStatus.InProgress.type] =
    Json.format[UploadStatus.InProgress.type]
  implicit val uploadedStatusFormat: OFormat[UploadStatus] = Json.format[UploadStatus]

  private implicit val referenceFormat: Format[Reference] =
    stringFormat[Reference](Reference(_), _.reference)

  implicit val uploadSuccessFormat: OFormat[UploadSuccess] = Json.format[UploadSuccess]
  implicit val validationErrorsFormat: OFormat[NonEmptyList[ValidationError]] =
    Json.format[NonEmptyList[ValidationError]]
  implicit val uploadErrorsFormat: OFormat[UploadErrors] = Json.format[UploadErrors]
  implicit val uploadFormatErrorFormat: OFormat[UploadFormatError.type] = Json.format[UploadFormatError.type]

  implicit val uploadFormat: OFormat[Upload] = Json.format[Upload]

  /*
  private[repositories] val mongoFormat: OFormat[MongoUpload] =
    (
      (__ \ "id").format[UploadKey] ~
        (__ \ "reference").format[Reference] ~
        (__ \ "status").format[UploadStatus] ~
        (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat) ~
        (__ \ "result").formatNullable[Upload]
    )(MongoUpload.apply, unlift(MongoUpload.unapply))
   */

  private def stringFormat[A](to: String => A, from: A => String): Format[A] =
    Format[A](Reads.StringReads.map(to), Writes.StringWrites.contramap(from))
}
