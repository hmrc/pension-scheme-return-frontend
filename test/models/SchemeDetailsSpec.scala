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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec
import play.api.libs.json.{JsString, Json}

class SchemeDetailsSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "SchemeDetails" - {

    "successfully read from json" in {

      forAll(schemeDetailsGen) { details =>
        Json.toJson(details).as[SchemeDetails] mustBe details
      }
    }
  }

  "SchemeStatus" - {

    "successfully read from json" in {
      forAll(schemeStatusGen) { status =>
        Json.toJson(status).as[SchemeStatus] mustBe status
      }
    }

    "return a JsError" - {
      "Scheme status is unknown" in {
        forAll(nonEmptyString) { status =>
          JsString(status).asOpt[SchemeStatus] mustBe None
        }
      }
    }
  }

  "ListSchemeDetails" - {

    "successfully read from json" in {
      forAll(listMinimalSchemeDetailsGen) { listMinimalSchemeDetails =>
        Json.toJson(listMinimalSchemeDetails).as[ListMinimalSchemeDetails] mustBe listMinimalSchemeDetails
      }
    }
  }
}
