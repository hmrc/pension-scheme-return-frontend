/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mongodb.scala.model.Updates.{combine, set}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs._
import play.api.libs.json._
import models._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import org.mongodb.scala.model.Filters.equal
import models.UploadKey.separator
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import config.{Crypto, FrontendAppConfig}
import cats.data.NonEmptyList
import models.SchemeId.asSrn
import models.UploadStatus.UploadStatus
import play.api.libs.functional.syntax._

import scala.Function.unlift
import scala.concurrent.{ExecutionContext, Future}

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

@Singleton
class UploadRepository @Inject() (
  mongoComponent: MongoComponent,
  clock: Clock,
  appConfig: FrontendAppConfig,
  crypto: Crypto
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[repositories.UploadRepository.MongoUpload](
      collectionName = "upload",
      mongoComponent = mongoComponent,
      domainFormat = repositories.UploadRepository.MongoUpload.format(crypto.getCrypto),
      indexes = Seq(
        IndexModel(Indexes.ascending("id"), IndexOptions().unique(true)),
        IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true)),
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.uploadTtl.toLong, TimeUnit.SECONDS)
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
      .head()
      .map(_ => ())

  def getUploadDetails(key: UploadKey): Future[Option[UploadDetails]] =
    collection.find(equal("id", key.toBson)).headOption().map(_.map(toUploadDetails))

  def updateStatus(reference: Reference, newStatus: UploadStatus): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("reference", reference.toBson),
        update = combine(
          set("status", SensitiveUploadStatus(newStatus).toBson),
          set("lastUpdated", Instant.now(clock).toBson)
        ),
        options = FindOneAndUpdateOptions().upsert(false)
      )
      .head()
      .map(_ => ())

  def setUploadResult(key: UploadKey, result: Upload): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("id", key.toBson),
        update = combine(
          set("result", SensitiveUpload(result).toBson),
          set("lastUpdated", Instant.now(clock).toBson)
        )
      )
      .head()
      .map(_ => ())

  def getUploadResult(key: UploadKey): Future[Option[Upload]] =
    collection
      .find(equal("id", key.value.toBson))
      .headOption()
      .map(_.flatMap(_.result.map(_.decryptedValue)))

  def remove(key: UploadKey): Future[Unit] =
    collection
      .deleteOne(equal("id", key.toBson))
      .head()
      .map(_ => ())

  private def toMongoUpload(details: UploadDetails): MongoUpload = MongoUpload(
    details.key,
    details.reference,
    SensitiveUploadStatus(details.status),
    details.lastUpdated,
    None
  )

  private def toUploadDetails(mongoUpload: MongoUpload): UploadDetails = UploadDetails(
    mongoUpload.key,
    mongoUpload.reference,
    mongoUpload.status.decryptedValue,
    mongoUpload.lastUpdated
  )
}

object UploadRepository {
  case class SensitiveUploadStatus(override val decryptedValue: UploadStatus) extends Sensitive[UploadStatus]

  case class SensitiveUpload(override val decryptedValue: Upload) extends Sensitive[Upload]

  implicit def sensitiveUploadFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveUpload] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUpload.apply)

  implicit def sensitiveUploadStatusFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveUploadStatus] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUploadStatus.apply)

  case class MongoUpload(
    key: UploadKey,
    reference: Reference,
    status: SensitiveUploadStatus,
    lastUpdated: Instant,
    result: Option[SensitiveUpload]
  )

  object MongoUpload {

    def reads(implicit crypto: Encrypter with Decrypter): Reads[MongoUpload] =
      (__ \ "id")
        .read[UploadKey]
        .and((__ \ "reference").read[Reference])
        .and((__ \ "status").read[SensitiveUploadStatus])
        .and((__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat))
        .and((__ \ "result").readNullable[SensitiveUpload])(MongoUpload.apply _)

    def writes(implicit crypto: Encrypter with Decrypter): OWrites[MongoUpload] =
      (__ \ "id")
        .write[UploadKey]
        .and((__ \ "reference").write[Reference])
        .and((__ \ "status").write[SensitiveUploadStatus])
        .and((__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat))
        .and((__ \ "result").writeNullable[SensitiveUpload])(
          unlift((x: MongoUpload) => Some(x._1, x._2, x._3, x._4, x._5))
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
  implicit val errorDetailsFormat: OFormat[ErrorDetails] = Json.format[ErrorDetails]
  implicit val uploadedFailedFormat: OFormat[UploadStatus.Failed] = Json.format[UploadStatus.Failed]
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
  implicit val uploadMaxRowsErrorFormat: OFormat[UploadMaxRowsError.type] = Json.format[UploadMaxRowsError.type]

  implicit val uploadFormat: OFormat[Upload] = Json.format[Upload]

  private def stringFormat[A](to: String => A, from: A => String): Format[A] =
    Format[A](Reads.StringReads.map(to), Writes.StringWrites.contramap(from))
}
