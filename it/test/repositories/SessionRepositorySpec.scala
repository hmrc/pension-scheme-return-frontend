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

import config.Constants.{PREVIOUS_SUBMITTED_PREFIX, UNCHANGED_SESSION_PREFIX}
import config.{FakeCrypto, FrontendAppConfig}
import models.UserAnswers
import models.UserAnswers.SensitiveJsObject
import org.mockito.Mockito.when
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.mongodb.scala.given

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec extends BaseRepositorySpec[UserAnswers] {

  private val id = "id"
  private val savedAnswers = jsObjectGen(maxDepth = 5).sample.value
  private val userAnswers = UserAnswers(id, SensitiveJsObject(savedAnswers), Instant.ofEpochSecond(1))
  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl).thenReturn(1)

  override protected val repository: SessionRepository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock,
    crypto = FakeCrypto
  )

  ".getBySrnAndIdNotEqual" - {
    "must return None if no documents exist" in {

      val getResult = repository.getBySrnAndIdNotEqual(id, srn).futureValue

      getResult mustBe None
    }

    "must return None if duplicated document doesn't exist" in {

      val userAnswersWitSrn1 = userAnswers.copy(id = id + srn)
      repository.set(userAnswersWitSrn1)

      val getResult = repository.getBySrnAndIdNotEqual(id, srn).futureValue

      getResult mustBe None
    }

    "must return None if only prefixed documents exist" in {

      val ua = userAnswers.copy(id = id + srn)
      val pureUa = userAnswers.copy(id = UNCHANGED_SESSION_PREFIX + id + srn)
      val previousUa = userAnswers.copy(id = PREVIOUS_SUBMITTED_PREFIX + id + srn)
      repository.set(ua)
      repository.set(pureUa)
      repository.set(previousUa)

      val getResult = repository.getBySrnAndIdNotEqual(id, srn).futureValue

      getResult mustBe None
    }
  }

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = userAnswers.copy(lastUpdated = instant)

      val setResult: Unit = repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value

      setResult mustEqual ()
      updatedRecord mustEqual expectedResult
    }
  }

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        insert(userAnswers).futureValue

        val result = repository.get(userAnswers.id).futureValue
        val expectedResult = userAnswers.copy(lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist").futureValue must not be defined
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      insert(userAnswers).futureValue

      val result: Unit = repository.clear(userAnswers.id).futureValue

      result mustEqual ()
      repository.get(userAnswers.id).futureValue must not be defined
    }

    "must return unit when there is no record to remove" in {
      val result: Unit = repository.clear("id that does not exist").futureValue

      result mustEqual ()
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return unit" in {

        insert(userAnswers).futureValue

        val result: Unit = repository.keepAlive(userAnswers.id).futureValue

        val expectedUpdatedAnswers = userAnswers.copy(lastUpdated = instant)

        result mustEqual ()
        val updatedAnswers = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return unit" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual ()
      }
    }
  }

  "encrypt data at rest" in {

    insert(userAnswers).futureValue
    val rawData =
      repository.collection
        .find[BsonDocument](Filters.equal("_id", userAnswers.id))
        .toFuture()
        .futureValue
        .headOption

    assert(rawData.nonEmpty)
    rawData.map(_.get("data").asString().getValue must fullyMatch.regex("^[A-Za-z0-9+/=]+$"))
  }
}
