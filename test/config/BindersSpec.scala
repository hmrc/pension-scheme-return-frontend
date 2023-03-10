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

package config

import config.Refined.OneToTen
import models.SchemeId.Srn
import org.scalacheck.Gen.alphaNumStr
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec
import org.scalacheck.Gen
import org.scalatest.EitherValues
import eu.timepit.refined.api.Refined

class BindersSpec extends BaseSpec with ScalaCheckPropertyChecks with EitherValues {

  "SRN binder" should {
    "return a valid srn" when {
      "srn is valid" in {
        forAll(srnGen) { validSrn =>
          Binders.srnBinder.bind("srn", validSrn.value) mustBe Right(validSrn)
        }
      }
    }

    "return an error message" when {
      "srn is invalid" in {
        forAll(alphaNumStr) { invalidSrn =>
          whenever(!invalidSrn.matches(Srn.srnRegex)) {
            Binders.srnBinder.bind("srn", invalidSrn) mustBe Left("Invalid scheme reference number")
          }
        }
      }
    }
  }

  "1 to 10 refined binder" should {
    "bind successfully when a number between 1 and 10 is supplied" in {
      forAll(Gen.oneOf(1 to 10)) { i =>
        Binders.refinedIntPathBinder[OneToTen].bind("max", i.toString) mustBe Right(Refined.unsafeApply(i))
      }
    }

    "fail to bind when a number is greater than 10" in {
      forAll(Gen.oneOf(11 to 30)) { i =>
        Binders.refinedIntPathBinder[OneToTen].bind("max", i.toString).isLeft mustBe true
      }
    }

    "fail to bind when a number is less than 1" in {
      forAll(Gen.oneOf(-10 to 0)) { i =>
        Binders.refinedIntPathBinder[OneToTen].bind("max", i.toString).isLeft mustBe true
      }
    }
  }
}