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
import models.UserAnswers.SensitiveJsObject
import controllers.TestValues
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._
import prepop.LoansProgressPrePopulationProcessorSpec.{baseReturnJsValue, cleanResultJsValue}

import scala.util.Success

class LoansProgressPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new LoansProgressPrePopulationProcessor()

  "LoansProgressPrePopulationProcessorSpec" - {

    "clean" - {
      "should cleanup the loans details from baseReturn and merge it onto currentUA" in {
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

object LoansProgressPrePopulationProcessorSpec {

  val baseReturnJsValue: JsValue =
    Json.parse("""
        |{
        |  "loansProgress" : {
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
        |  "loansProgress" : {
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
