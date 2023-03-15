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

package services

import connectors.SchemeDetailsConnector
import models.SchemeId.Srn
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class SchemeDetailsServiceSpec extends BaseSpec with ScalaCheckPropertyChecks {

  implicit val hc = HeaderCarrier()

  val mockSchemeDetailsConnector = mock[SchemeDetailsConnector]
  val service = new SchemeDetailsServiceImpl(mockSchemeDetailsConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSchemeDetailsConnector)
  }

  "getMinimalDetails" should {

    "return correct minimal scheme details for psa" in {

      forAll(listMinimalSchemeDetailsGen, psaIdGen) { (details, psaId) =>
        whenever(details.schemeDetails.nonEmpty) {

          val expectedDetails = Random.shuffle(details.schemeDetails).head
          val srn = Srn(expectedDetails.srn).value

          when(mockSchemeDetailsConnector.listSchemeDetails(meq(psaId))(any(), any()))
            .thenReturn(Future.successful(Some(details)))

          service.getMinimalSchemeDetails(psaId, srn).futureValue mustBe Some(expectedDetails)
        }
      }
    }

    "return correct minimal scheme details for psp" in {

      forAll(listMinimalSchemeDetailsGen, pspIdGen) { (details, pspId) =>
        whenever(details.schemeDetails.nonEmpty) {

          val expectedDetails = Random.shuffle(details.schemeDetails).head
          val srn = Srn(expectedDetails.srn).value

          when(mockSchemeDetailsConnector.listSchemeDetails(meq(pspId))(any(), any()))
            .thenReturn(Future.successful(Some(details)))

          service.getMinimalSchemeDetails(pspId, srn).futureValue mustBe Some(expectedDetails)
        }
      }
    }

    "return none when srn not found for psa" in {

      forAll(listMinimalSchemeDetailsGen, psaIdGen, srnGen) { (details, psaId, srn) =>
        whenever(!details.schemeDetails.exists(_.srn == srn.value)) {

          when(mockSchemeDetailsConnector.listSchemeDetails(meq(psaId))(any(), any()))
            .thenReturn(Future.successful(Some(details)))

          service.getMinimalSchemeDetails(psaId, srn).futureValue mustBe None
        }
      }
    }

    "return none when srn not found for psp" in {

      forAll(listMinimalSchemeDetailsGen, pspIdGen, srnGen) { (details, pspId, srn) =>
        whenever(!details.schemeDetails.exists(_.srn == srn.value)) {

          when(mockSchemeDetailsConnector.listSchemeDetails(meq(pspId))(any(), any()))
            .thenReturn(Future.successful(Some(details)))

          service.getMinimalSchemeDetails(pspId, srn).futureValue mustBe None
        }
      }
    }

    "return none when connector returns none for psa" in {

      forAll(psaIdGen, srnGen) { (psaId, srn) =>
        when(mockSchemeDetailsConnector.listSchemeDetails(meq(psaId))(any(), any()))
          .thenReturn(Future.successful(None))

        service.getMinimalSchemeDetails(psaId, srn).futureValue mustBe None
      }
    }

    "return none when connector returns none for psp" in {

      forAll(pspIdGen, srnGen) { (pspId, srn) =>
        when(mockSchemeDetailsConnector.listSchemeDetails(meq(pspId))(any(), any()))
          .thenReturn(Future.successful(None))

        service.getMinimalSchemeDetails(pspId, srn).futureValue mustBe None
      }
    }
  }
}
