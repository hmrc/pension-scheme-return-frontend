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

import config.{FakeCrypto, FrontendAppConfig}
import models._
import org.mockito.Mockito.when
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import repositories.UploadRepository.MongoUpload
import repositories.UploadRepository.MongoUpload.SensitiveUploadStatus

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class UploadRepositorySpec extends BaseRepositorySpec[MongoUpload] {

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.uploadTtl).thenReturn(1)

  private val oldInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS).minusMillis(1000)
  private val initialUploadDetails = UploadDetails(uploadKey, reference, UploadStatus.InProgress, oldInstant)
  private val initialMongoUpload =
    MongoUpload(uploadKey, reference, SensitiveUploadStatus(UploadStatus.InProgress), oldInstant, None)
  private val encryptedRegex = "^[A-Za-z0-9+/=]+$"

  override protected val repository = new UploadRepository(
    mongoComponent,
    stubClock,
    mockAppConfig,
    crypto = FakeCrypto
  )

  ".insert" - {
    "successfully insert UploadDetails" in {
      val insertResult: Unit = repository.insert(initialUploadDetails).futureValue
      val findResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      insertResult mustBe ()
      findResult mustBe initialMongoUpload
    }
  }

  ".find" - {
    "successfully fetch a previously inserted UploadDetails" in {
      insertInitialUploadDetails()
    }
  }

  ".updateStatus" - {
    "successfully update the ttl and status to failed" in {
      insertInitialUploadDetails()

      val updateResult: Unit = repository.updateStatus(reference, failure).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      updateResult mustBe ()
      findAfterUpdateResult mustBe initialMongoUpload.copy(
        status = SensitiveUploadStatus(failure),
        lastUpdated = instant
      )
    }

    "successfully update the ttl and status to success" in {
      insertInitialUploadDetails()

      val uploadSuccessful = UploadStatus.Success("test-name", "text/csv", "/test-url", None)
      val updateResult: Unit = repository.updateStatus(reference, uploadSuccessful).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      updateResult mustBe ()
      findAfterUpdateResult mustBe initialMongoUpload.copy(
        status = SensitiveUploadStatus(uploadSuccessful),
        lastUpdated = instant
      )
    }

    "successfully encrypt status" in {
      insertInitialUploadDetails()

      val uploadSuccessful = UploadStatus.Success("test-name", "text/csv", "/test-url", None)
      repository.updateStatus(reference, uploadSuccessful).futureValue

      val rawData =
        repository.collection
          .find[BsonDocument](Filters.equal("id", uploadKey.value))
          .toFuture()
          .futureValue
          .headOption

      assert(rawData.nonEmpty)
      rawData.map(_.get("status").asString().getValue must fullyMatch.regex(encryptedRegex))
    }

  }

  ".setUploadResult" - {
    "successfully update the ttl and upload result to UploadFormatError" in {
      insertInitialUploadDetails()

      val updateResult: Unit = repository.setUploadResult(uploadKey, UploadFormatError).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      updateResult mustBe ()
      findAfterUpdateResult.lastUpdated mustBe instant
      findAfterUpdateResult.result.value.decryptedValue mustBe UploadFormatError
    }
  }

  ".getUploadResult" - {
    "successfully get the upload result" in {
      insertInitialUploadDetails()

      val updateResult: Unit = repository.setUploadResult(uploadKey, UploadFormatError).futureValue
      val getResult = repository.getUploadResult(uploadKey).futureValue

      updateResult mustBe ()
      getResult mustBe Some(UploadFormatError)
    }
  }

  ".remove" - {
    "successfully remove a previously inserted record" in {
      insertInitialUploadDetails()

      val removeResult: Unit = repository.remove(uploadKey).futureValue
      val findAfterRemoveResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption

      removeResult mustBe ()
      findAfterRemoveResult mustBe None
    }
  }

  "successfully encrypt result" in {
    insertInitialUploadDetails()

    repository.setUploadResult(uploadKey, UploadFormatError).futureValue
    val rawData =
      repository.collection
        .find[BsonDocument](Filters.equal("id", uploadKey.value))
        .toFuture()
        .futureValue
        .headOption

    assert(rawData.nonEmpty)
    rawData.map(_.get("result").asString().getValue must fullyMatch.regex(encryptedRegex))
  }

  private def insertInitialUploadDetails(): Unit = {
    val insertResult: Unit = repository.insert(initialUploadDetails).futureValue
    val findResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

    insertResult mustBe ()
    findResult mustBe initialMongoUpload
  }
}
