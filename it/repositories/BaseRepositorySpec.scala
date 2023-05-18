package repositories

import generators.Generators
import models.{Reference, SchemeId, UploadKey}
import org.mockito.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import java.time.temporal.ChronoUnit

trait BaseRepositorySpec[A] extends AnyFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[A]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar
  with Generators {

  val srn: SchemeId.Srn = srnGen.sample.value
  val uploadKey: UploadKey = UploadKey("test-userid", srn)
  val reference: Reference = Reference("test-ref")
  val instant: Instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
}
