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

package models

import models.SchemeId.Srn
import org.scalacheck.Gen.alphaNumStr
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec

class SchemeIdSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "Srn" - {

    "return Srn for valid value" in {
      forAll(srnGen) { validSrn =>
        Srn(validSrn.value) mustBe Some(validSrn)
      }
    }

    "return None for invalid value" in {
      forAll(alphaNumStr) { invalidSrn =>
        whenever(!invalidSrn.matches(Srn.srnRegex)) {
          Srn(invalidSrn) mustBe None
        }
      }
    }
  }
}
