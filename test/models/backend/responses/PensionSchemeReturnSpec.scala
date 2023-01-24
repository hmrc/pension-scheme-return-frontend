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

package models.backend.responses

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class PensionSchemeReturnSpec extends AnyWordSpec with Matchers {

  "PensionSchemeReturn" should {
    List(
      (DataEntryRule.Fixed, "fixed"),
      (DataEntryRule.Updated, "updated"),
      (DataEntryRule.None, "none")
    ).foreach { case (rule, ruleSerialised) =>
      s"serialise to json correctly for rule $rule" in {
        val pensionSchemeReturn = PensionSchemeReturn(
          name = DataEntry(
            "testName",
            rule,
            DataEntryChanged("v1", "testPreviousName")
          )
        )

        val expectedJson = Json.obj(
          "name" -> Json.obj(
            "value" -> "testName",
            "rule" -> ruleSerialised,
            "changed" -> Json.obj(
              "version" -> "v1",
              "previousValue" -> "testPreviousName"
            )
          )
        )

        expectedJson.as[PensionSchemeReturn] shouldBe pensionSchemeReturn
      }
    }
  }
}
