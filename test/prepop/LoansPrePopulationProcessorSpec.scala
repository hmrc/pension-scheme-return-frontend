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
import prepop.LoansPrePopulationProcessorSpec._
import controllers.TestValues
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._

import scala.util.Success

class LoansPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new LoansPrePopulationProcessor()

  "LoansPrePopulationProcessorSpec" - {

    "clean" - {
      "should cleanup the loans details from baseReturn and merge it onto currentUA" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(baseReturnJsValue.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result.get.data.decryptedValue mustBe cleanResultJsValue.as[JsObject]
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanResultJsValue.as[JsObject]))
        )
      }

      "should not copy schemeHadLoans from empty loans" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(baseReturnNoLoansJsValue.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result.get.data.decryptedValue mustBe cleanResultNoLoansJsValue.as[JsObject]
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanResultNoLoansJsValue.as[JsObject]))
        )
      }
    }
  }
}

object LoansPrePopulationProcessorSpec {

  val baseReturnJsValue: JsValue =
    Json.parse("""
        |{
        |  "loans" : {
        |    "recordVersion" : "004",
        |    "schemeHadLoans" : true,
        |    "loanTransactions" : {
        |      "recipientIdentityType" : {
        |        "identityTypes" : {
        |          "0" : "ukCompany",
        |          "1" : "ukCompany"
        |        },
        |        "crn" : {
        |          "0" : {
        |            "no" : "XYZ not on record."
        |          },
        |          "1" : {
        |            "no" : "Identification not on record."
        |          }
        |        }
        |      },
        |      "loanRecipientName" : {
        |        "company" : {
        |          "0" : "Acquisition Loans Ltd.",
        |          "1" : "Acquisition Loan Recipient Ltd."
        |        }
        |      },
        |      "recipientSponsoringEmployerConnectedParty" : {
        |        "0" : "sponsoring",
        |        "1" : "sponsoring"
        |      },
        |      "datePeriodLoanPage" : {
        |        "0" : [ "2023-05-30", {
        |          "value" : 100,
        |          "displayAs" : "100.00"
        |        }, 10 ],
        |        "1" : [ "2023-04-30", {
        |          "value" : 200,
        |          "displayAs" : "200.00"
        |        }, 12 ]
        |      },
        |      "loanAmountPage" : {
        |        "0" : {
        |          "loanAmount" : {
        |            "value" : 2342322,
        |            "displayAs" : "2,342,322.00"
        |          },
        |          "optCapRepaymentCY" : {
        |            "value" : 5000,
        |            "displayAs" : "5,000.00"
        |          },
        |          "optAmountOutstanding" : {
        |            "value" : 500,
        |            "displayAs" : "500.00"
        |          }
        |        },
        |        "1" : {
        |          "loanAmount" : {
        |            "value" : 1000,
        |            "displayAs" : "1,000.00"
        |          },
        |          "optCapRepaymentCY" : {
        |            "value" : 5000,
        |            "displayAs" : "5,000.00"
        |          },
        |          "optAmountOutstanding" : {
        |            "value" : 500,
        |            "displayAs" : "500.00"
        |          }
        |        }
        |      },
        |      "equalInstallments" : {
        |        "0" : false,
        |        "1" : false
        |      },
        |      "loanInterestPage" : {
        |        "0" : {
        |          "loanInterestAmount" : {
        |            "value" : 100,
        |            "displayAs" : "100.00"
        |          },
        |          "optIntReceivedCY" : {
        |            "value" : 100,
        |            "displayAs" : "100.00"
        |          },
        |          "loanInterestRate" : {
        |            "value" : 1.55,
        |            "displayAs" : "1.55"
        |          }
        |        },
        |        "1" : {
        |          "optIntReceivedCY" : {
        |            "value" : 100,
        |            "displayAs" : "100.00"
        |          },
        |          "loanInterestAmount" : {
        |            "value" : 200,
        |            "displayAs" : "200.00"
        |          },
        |          "loanInterestRate" : {
        |            "value" : 2.55,
        |            "displayAs" : "2.55"
        |          }
        |        }
        |      },
        |      "securityGivenPage" : {
        |        "0" : {
        |          "yes" : "Asian vase 344343444"
        |        },
        |        "1" : {
        |          "yes" : "Asian vase 344343444"
        |        }
        |      },
        |      "outstandingArrearsOnLoan" : {
        |        "0" : {
        |          "yes" : {
        |            "value" : 456,
        |            "displayAs" : "456.00"
        |          }
        |        },
        |        "1" : {
        |          "yes" : {
        |            "value" : 456,
        |            "displayAs" : "456.00"
        |          }
        |        }
        |      }
        |    }
        |  }
        |}
        |""".stripMargin)

  val baseReturnNoLoansJsValue: JsValue =
    Json.parse("""
        |{
        |  "loans": {
        |    "recordVersion" : "004",
        |    "schemeHadLoans" : false
        |  }
        |}""".stripMargin)

  val cleanResultJsValue: JsValue =
    Json.parse("""
        |{
        |  "current": "dummy-current-data",
        |  "loans": {
        |    "loanPrePopulated": {
        |      "0": false,
        |      "1": false
        |    }, 
        |    "loanTransactions": {
        |      "recipientIdentityType": {
        |        "identityTypes": {
        |          "0": "ukCompany",
        |          "1": "ukCompany"
        |        },
        |        "crn": {
        |          "0": {
        |            "no": "XYZ not on record."
        |          },
        |          "1": {
        |            "no": "Identification not on record."
        |          }
        |        }
        |      },
        |      "loanRecipientName": {
        |        "company": {
        |          "0": "Acquisition Loans Ltd.",
        |          "1": "Acquisition Loan Recipient Ltd."
        |        }
        |      },
        |      "recipientSponsoringEmployerConnectedParty": {
        |        "0": "sponsoring",
        |        "1": "sponsoring"
        |      },
        |      "datePeriodLoanPage": {
        |        "0": [
        |          "2023-05-30",
        |          {
        |            "value": 100,
        |            "displayAs": "100.00"
        |          },
        |          10
        |        ],
        |        "1": [
        |          "2023-04-30",
        |          {
        |            "value": 200,
        |            "displayAs": "200.00"
        |          },
        |          12
        |        ]
        |      },
        |      "loanAmountPage": {
        |        "0": {
        |          "loanAmount": {
        |            "value": 2342322,
        |            "displayAs": "2,342,322.00"
        |          }
        |        },
        |        "1": {
        |          "loanAmount": {
        |            "value": 1000,
        |            "displayAs": "1,000.00"
        |          }
        |        }
        |      },
        |      "equalInstallments": {
        |        "0": false,
        |        "1": false
        |      },
        |      "loanInterestPage": {
        |        "0": {
        |          "loanInterestAmount": {
        |            "value": 100,
        |            "displayAs": "100.00"
        |          },
        |          "loanInterestRate": {
        |            "value": 1.55,
        |            "displayAs": "1.55"
        |          }
        |        },
        |        "1": {
        |          "loanInterestAmount": {
        |            "value": 200,
        |            "displayAs": "200.00"
        |          },
        |          "loanInterestRate": {
        |            "value": 2.55,
        |            "displayAs": "2.55"
        |          }
        |        }
        |      },
        |      "securityGivenPage": {
        |        "0": {
        |          "yes": "Asian vase 344343444"
        |        },
        |        "1": {
        |          "yes": "Asian vase 344343444"
        |        }
        |      }
        |    }
        |  }
        |}
        |""".stripMargin)

  val cleanResultNoLoansJsValue: JsValue =
    Json.parse("""
        |{
        |  "current": "dummy-current-data"
        |}""".stripMargin)
}
