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

package config

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec
import models.SchemeId.Srn
import org.scalacheck.Gen.alphaNumStr
import org.scalatest.EitherValues
import models.IdentitySubject
import models.IdentitySubject._

class BindersSpec extends BaseSpec with ScalaCheckPropertyChecks with EitherValues {

  "SRN binder" - {
    "return a valid srn" - {
      "srn is valid" in {
        forAll(srnGen) { validSrn =>
          Binders.srnBinder.bind("srn", validSrn.value) mustBe Right(validSrn)
        }
      }
    }

    "return an error message" - {
      "srn is invalid" in {
        forAll(alphaNumStr) { invalidSrn =>
          whenever(!invalidSrn.matches(Srn.srnRegex)) {
            Binders.srnBinder.bind("srn", invalidSrn) mustBe Left("Invalid scheme reference number")
          }
        }
      }
    }
  }

  "IdentitySubject binder" - {
    "should return a valid identity subject" - {
      "when identity subject is valid" in {
        List(
          LoanRecipient,
          LandOrPropertySeller,
          SharesSeller,
          OtherAssetSeller,
          Unknown
        ).foreach { identitySubject =>
          val x = Binders.identitySubjectBinder.bind("srn", identitySubject.name)
          x.value mustBe identitySubject
        }
      }
    }

    "should return Unknow" - {
      "when identity subject is invalid" in {
        Binders.identitySubjectBinder.bind("key", "invalid") mustBe Right(IdentitySubject.Unknown)
      }
    }
  }
}
