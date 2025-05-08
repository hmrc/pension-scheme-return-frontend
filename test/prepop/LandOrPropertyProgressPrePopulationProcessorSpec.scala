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

package prepop

import utils.BaseSpec
import prepop.LandOrPropertyProgressPrePopulationProcessorSpec.{baseReturnJsValue, cleanResultJsValue}
import models.UserAnswers.SensitiveJsObject
import controllers.TestValues
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._

import scala.util.Success

class LandOrPropertyProgressPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new LandOrPropertyProgressPrePopulationProcessor()

  "LandOrPropertyProgressPrePopulationProcessorSpec" - {

    "clean" - {
      "should cleanup the land or property details from baseReturn and merge it onto currentUA" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(baseReturnJsValue.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanResultJsValue.as[JsObject]))
        )
      }
    }
  }
}

object LandOrPropertyProgressPrePopulationProcessorSpec {

  val baseReturnJsValue: JsValue =
    Json.parse("""
        |{
        |  "landOrPropertyProgress" : {
        |    "0" : {
        |      "status" : "JourneyCompleted"
        |    },
        |    "1" : {
        |      "status" : "JourneyCompleted"
        |    }
        |  }
        |}
        |""".stripMargin)

  val cleanResultJsValue: JsValue =
    Json.parse("""
        |{
        |  "current": "dummy-current-data",
        |  "landOrPropertyProgress" : {
        |    "0" : {
        |      "status" : "JourneyCompleted"
        |    },
        |    "1" : {
        |      "status" : "JourneyCompleted"
        |    }
        |  }
        |}
        |""".stripMargin)
}
