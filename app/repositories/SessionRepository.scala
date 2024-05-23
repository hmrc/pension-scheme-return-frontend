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

import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.model._
import config.Constants.UNCHANGED_SESSION_PREFIX
import org.mongodb.scala.bson.conversions.Bson
import play.api.libs.json.Format
import models.UserAnswers
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.FutureUtils.FutureOps
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import config.{Crypto, FrontendAppConfig}

import scala.concurrent.{ExecutionContext, Future}

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

@Singleton
class SessionRepository @Inject()(
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig,
  clock: Clock,
  crypto: Crypto
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UserAnswers](
      collectionName = "user-answers",
      mongoComponent = mongoComponent,
      domainFormat = UserAnswers.format(crypto.getCrypto),
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = false
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def byIdWithPureSession(id: String): Bson = {
    val filters = List(id, UNCHANGED_SESSION_PREFIX + id).map(byId)
    Filters.or(filters: _*)
  }

  def keepAlive(id: String): Future[Unit] =
    collection
      .updateMany(
        filter = byIdWithPureSession(id),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .as(())

  def get(id: String): Future[Option[UserAnswers]] =
    keepAlive(id).flatMap { _ =>
      collection
        .find(byId(id))
        .headOption()
    }

  def set(answers: UserAnswers): Future[Unit] = {

    val updatedAnswers = answers.copy(lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byId(updatedAnswers.id),
        replacement = updatedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .as(())
  }

  def clear(id: String): Future[Unit] =
    collection
      .deleteMany(byIdWithPureSession(id))
      .toFuture()
      .as(())
}
