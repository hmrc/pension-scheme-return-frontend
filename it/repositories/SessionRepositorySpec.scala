package repositories

import config.{FakeCrypto, FrontendAppConfig}
import generators.Generators
import models.UserAnswers
import models.UserAnswers.SensitiveJsObject
import org.mockito.Mockito.when
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with Generators {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val savedAnswers = jsObjectGen(maxDepth = 5).sample.value
  private val userAnswers = UserAnswers("id", SensitiveJsObject(savedAnswers), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock,
    crypto         = FakeCrypto
  )

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = userAnswers copy (lastUpdated = instant)

      val setResult     = repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value

      setResult mustEqual ()
      updatedRecord mustEqual expectedResult
    }
  }

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        insert(userAnswers).futureValue

        val result         = repository.get(userAnswers.id).futureValue
        val expectedResult = userAnswers copy (lastUpdated = instant)

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

      val result = repository.clear(userAnswers.id).futureValue

      result mustEqual ()
      repository.get(userAnswers.id).futureValue must not be defined
    }

    "must return unit when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual ()
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return unit" in {

        insert(userAnswers).futureValue

        val result = repository.keepAlive(userAnswers.id).futureValue

        val expectedUpdatedAnswers = userAnswers copy (lastUpdated = instant)

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
      repository
        .collection
        .find[BsonDocument](Filters.equal("_id", userAnswers.id))
        .toFuture()
        .futureValue
        .headOption

    assert(rawData.nonEmpty)
    rawData.map(_.get("data").asString().getValue must fullyMatch.regex("^[A-Za-z0-9+/=]+$"))
  }
}
