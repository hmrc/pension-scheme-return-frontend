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

package utils.nonsipp

import play.api.test.FakeRequest
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import org.scalatest.OptionValues
import models.requests.DataRequest
import utils.nonsipp.PrePopulationUtils._
import config.Constants.PREPOPULATION_FLAG

class PrePopulationUtilsSpec extends ControllerBaseSpec with ControllerBehaviours with Matchers with OptionValues {

  implicit val request: DataRequest[AnyContentAsEmpty.type] =
    DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers)
  implicit val requestWithFlagTrue: DataRequest[AnyContentAsEmpty.type] = DataRequest(
    allowedAccessRequestGen(
      FakeRequest()
        .withSession((PREPOPULATION_FLAG, "true"))
    ).sample.value,
    defaultUserAnswers
  )
  implicit val requestWithFlagFalse: DataRequest[AnyContentAsEmpty.type] = DataRequest(
    allowedAccessRequestGen(
      FakeRequest()
        .withSession((PREPOPULATION_FLAG, "false"))
    ).sample.value,
    defaultUserAnswers
  )

  "isPrePopulation" - {

    "must be false" - {

      "when no PREPOPULATION_FLAG flag present" in {
        isPrePopulation(request) mustBe false
      }

      "when no PREPOPULATION_FLAG flag is false" in {
        isPrePopulation(requestWithFlagFalse) mustBe false
      }
    }

    "must be true" - {

      "when no PREPOPULATION_FLAG flag is false" in {
        isPrePopulation(requestWithFlagTrue) mustBe true
      }
    }
  }
}
