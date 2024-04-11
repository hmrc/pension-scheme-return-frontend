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

import generators.Generators
import models.{ErrorDetails, Reference, SchemeId, UploadKey, UploadStatus}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.{DefaultPlayMongoRepositorySupport, TtlIndexedMongoSupport}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}

trait BaseRepositorySpec[A]
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[A]
    with TtlIndexedMongoSupport
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
  val failure: UploadStatus.Failed = UploadStatus.Failed(ErrorDetails("reason", "message"))
}
