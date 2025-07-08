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
import models.SchemeId.Srn
import config.Constants.{PREVIOUS_SUBMITTED_PREFIX, UNCHANGED_SESSION_PREFIX}
import org.mongodb.scala.bson.conversions.Bson
import play.api.libs.json.Format
import models.UserAnswers
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.FutureUtils.FutureOps
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import config.{Crypto, FrontendAppConfig}

import scala.concurrent.{ExecutionContext, Future}

import java.time.{Clock, Instant}
import java.util.regex.Pattern
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

@Singleton
class SessionRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig,
  clock: Clock,
  crypto: Crypto
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UserAnswers](
      collectionName = "user-answers",
      mongoComponent = mongoComponent,
      domainFormat = UserAnswers.format(using crypto.getCrypto),
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.cacheTtl.toLong, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = false
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def idNotEqual(id: String): Bson = Filters.not(Filters.equal("_id", id))

  private def sessionIds(id: String) =
    List(id, UNCHANGED_SESSION_PREFIX + id, PREVIOUS_SUBMITTED_PREFIX + id)

  private def bySessionIds(id: String): Bson = {
    val filters = sessionIds(id).map(byId)
    Filters.or(filters*)
  }

  private def byIdNotInSessionIdsList(id: String): Bson = {
    val filters = sessionIds(id).map(idNotEqual)
    Filters.and(filters*)
  }

  def keepAlive(id: String): Future[Unit] =
    collection
      .updateMany(
        filter = bySessionIds(id),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .head()
      .as(())

  def get(id: String): Future[Option[UserAnswers]] =
    keepAlive(id).flatMap { _ =>
      collection
        .find(byId(id))
        .headOption()
    }

  def getBySrnAndIdNotEqual(userId: String, srn: Srn): Future[Option[UserAnswers]] = {
    val userIdKey = userId + srn
    val hits = collection
      .find(
        Filters.and(
          byIdNotInSessionIdsList(userIdKey),
          Filters.regex("_id", "^.*" + Pattern.quote(srn.value) + ".*$", "i")
        )
      )
      .map { doc =>
        UserAnswers(id = doc.id, data = doc.data, lastUpdated = doc.lastUpdated)
      }
    hits.headOption()
  }

  def set(answers: UserAnswers): Future[Unit] = {

    val updatedAnswers = answers.copy(lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byId(updatedAnswers.id),
        replacement = updatedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .head()
      .as(())
  }

  def clear(id: String): Future[Unit] =
    collection
      .deleteMany(bySessionIds(id))
      .head()
      .as(())
}
