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
import prepop.MemberPrePopulationProcessorSpec.{baseReturn, cleanResultJsValue}
import controllers.TestValues
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._

import scala.util.Success

class MemberPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new MemberPrePopulationProcessor()

  "MemberPrePopulationProcessor" - {

    "clean" - {
      "should cleanup the Member details from baseReturn and put it onto currentUA" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(baseReturn.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanResultJsValue.as[JsObject]))
        )
      }
    }
  }
}

object MemberPrePopulationProcessorSpec {

  val baseReturn: JsValue =
    Json.parse("""
        |{
        |  "membersPayments": {
        |    "recordVersion": "004",
        |    "unallocatedContribsMade": true,
        |    "unallocatedContribAmount": {
        |      "value": 123.45,
        |      "displayAs": "123.45"
        |    },
        |    "memberDetails": {
        |      "personalDetails": {
        |        "nameDob": {
        |          "0": {
        |            "firstName": "Ferdinand",
        |            "lastName": "Bull",
        |            "dob": "1960-05-31"
        |          },
        |          "1": {
        |            "firstName": "Johnny",
        |            "lastName": "Quicke",
        |            "dob": "1940-10-31"
        |          },
        |          "2": {
        |            "firstName": "Foury",
        |            "lastName": "Lasty",
        |            "dob": "1940-10-31"
        |          },
        |          "3": {
        |            "firstName": "Five",
        |            "lastName": "Last",
        |            "dob": "1940-10-31"
        |          },
        |          "4": {
        |            "firstName": "Six",
        |            "lastName": "User",
        |            "dob": "1949-12-31"
        |          },
        |          "5": {
        |            "firstName": "NewUser",
        |            "lastName": "LastName",
        |            "dob": "1940-10-31"
        |          }
        |        },
        |        "nationalInsuranceNumber": {
        |          "0": true,
        |          "1": false,
        |          "2": false,
        |          "3": false,
        |          "4": false,
        |          "5": false
        |        },
        |        "nino": {
        |          "0": "EB103145A"
        |        },
        |        "memberDetailsSectionCompleted": {
        |          "0": {},
        |          "1": {},
        |          "2": {},
        |          "3": {},
        |          "4": {},
        |          "5": {}
        |        },
        |        "memberStatus": {
        |          "0": "Changed",
        |          "1": "Changed",
        |          "2": "Changed",
        |          "3": "New",
        |          "4": "New",
        |          "5": "New"
        |        },
        |        "noNINO": {
        |          "1": "Could not find it on record.",
        |          "2": "Changed",
        |          "3": "Could not find it on record.",
        |          "4": "Could not find it on record.",
        |          "5": "Changed"
        |        }
        |      },
        |      "memberEmpContribution": {
        |        "orgName": {
        |          "0": {
        |            "0": "Acme Ltd",
        |            "1": "UK Company Ltd"
        |          },
        |          "1": {
        |            "0": "Sofa Inc.",
        |            "1": "UK Company XYZ Ltd."
        |          },
        |          "2": {
        |            "0": "Sofa Inc.",
        |            "1": "UK Company XYZ Ltd."
        |          },
        |          "3": {
        |            "0": "Sofa Inc.",
        |            "1": "UK Company XYZ Ltd."
        |          },
        |          "4": {
        |            "0": "Chair Inc.",
        |            "1": "UK Company ABC Ltd."
        |          },
        |          "5": {
        |            "0": "Sofa Inc.",
        |            "1": "UK Company XYZ Ltd."
        |          }
        |        },
        |        "totalContribution": {
        |          "0": {
        |            "0": {
        |              "value": 20000,
        |              "displayAs": "20,000.00"
        |            },
        |            "1": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            }
        |          },
        |          "1": {
        |            "0": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            },
        |            "1": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            }
        |          },
        |          "2": {
        |            "0": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            },
        |            "1": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            }
        |          },
        |          "3": {
        |            "0": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            },
        |            "1": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            }
        |          },
        |          "4": {
        |            "0": {
        |              "value": 100000,
        |              "displayAs": "100,000.00"
        |            },
        |            "1": {
        |              "value": 100000,
        |              "displayAs": "100,000.00"
        |            }
        |          },
        |          "5": {
        |            "0": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            },
        |            "1": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            }
        |          }
        |        },
        |        "orgType": {
        |          "0": {
        |            "0": "ukCompany",
        |            "1": "ukCompany"
        |          },
        |          "1": {
        |            "0": "other",
        |            "1": "ukCompany"
        |          },
        |          "2": {
        |            "0": "other",
        |            "1": "ukCompany"
        |          },
        |          "3": {
        |            "0": "other",
        |            "1": "ukCompany"
        |          },
        |          "4": {
        |            "0": "other",
        |            "1": "ukCompany"
        |          },
        |          "5": {
        |            "0": "other",
        |            "1": "ukCompany"
        |          }
        |        },
        |        "idNumber": {
        |          "0": {
        |            "0": {
        |              "yes": "AC123456"
        |            },
        |            "1": {
        |              "yes": "AC123456"
        |            }
        |          },
        |          "1": {
        |            "1": {
        |              "yes": "CC123456"
        |            }
        |          },
        |          "2": {
        |            "1": {
        |              "yes": "CC123456"
        |            }
        |          },
        |          "3": {
        |            "1": {
        |              "yes": "CC123456"
        |            }
        |          },
        |          "4": {
        |            "1": {
        |              "yes": "CC123456"
        |            }
        |          },
        |          "5": {
        |            "1": {
        |              "yes": "CC123456"
        |            }
        |          }
        |        },
        |        "otherDescription": {
        |          "1": {
        |            "0": "Found it down back of my sofa"
        |          },
        |          "2": {
        |            "0": "Found it down back of my sofa"
        |          },
        |          "3": {
        |            "0": "Found it down back of my sofa"
        |          },
        |          "4": {
        |            "0": "Found it down back of my chair"
        |          },
        |          "5": {
        |            "0": "Found it down back of my sofa"
        |          }
        |        }
        |      },
        |      "memberTransfersIn": {
        |        "schemeName": {
        |          "0": {
        |            "0": "The Happy Retirement Scheme",
        |            "1": "The Happy Retirement Scheme"
        |          },
        |          "1": {
        |            "0": "Golden Years Pension Scheme",
        |            "1": "Golden Goose Egg Laying Scheme"
        |          },
        |          "2": {
        |            "0": "Golden Years Pension Scheme",
        |            "1": "Golden Goose Egg Laying Scheme"
        |          },
        |          "3": {
        |            "0": "Golden Years Pension Scheme",
        |            "1": "Golden Goose Egg Laying Scheme"
        |          },
        |          "4": {
        |            "0": "Silver Years Pension Scheme",
        |            "1": "Silver Goose Egg Laying Scheme"
        |          },
        |          "5": {
        |            "0": "Golden Years Pension Scheme",
        |            "1": "Golden Goose Egg Laying Scheme"
        |          }
        |        },
        |        "dateOfTransfer": {
        |          "0": {
        |            "0": "2022-08-08",
        |            "1": "2022-11-27"
        |          },
        |          "1": {
        |            "0": "2022-12-02",
        |            "1": "2022-10-30"
        |          },
        |          "2": {
        |            "0": "2022-12-02",
        |            "1": "2022-10-30"
        |          },
        |          "3": {
        |            "0": "2022-12-02",
        |            "1": "2022-10-30"
        |          },
        |          "4": {
        |            "0": "2022-12-02",
        |            "1": "2022-10-30"
        |          },
        |          "5": {
        |            "0": "2022-12-02",
        |            "1": "2022-10-30"
        |          }
        |        },
        |        "transferValue": {
        |          "0": {
        |            "0": {
        |              "value": 10000,
        |              "displayAs": "10,000.00"
        |            },
        |            "1": {
        |              "value": 8000,
        |              "displayAs": "8,000.00"
        |            }
        |          },
        |          "1": {
        |            "0": {
        |              "value": 50000,
        |              "displayAs": "50,000.00"
        |            },
        |            "1": {
        |              "value": 2000,
        |              "displayAs": "2,000.00"
        |            }
        |          },
        |          "2": {
        |            "0": {
        |              "value": 50000,
        |              "displayAs": "50,000.00"
        |            },
        |            "1": {
        |              "value": 2000,
        |              "displayAs": "2,000.00"
        |            }
        |          },
        |          "3": {
        |            "0": {
        |              "value": 50000,
        |              "displayAs": "50,000.00"
        |            },
        |            "1": {
        |              "value": 2000,
        |              "displayAs": "2,000.00"
        |            }
        |          },
        |          "4": {
        |            "0": {
        |              "value": 50000,
        |              "displayAs": "50,000.00"
        |            },
        |            "1": {
        |              "value": 2000,
        |              "displayAs": "2,000.00"
        |            }
        |          },
        |          "5": {
        |            "0": {
        |              "value": 50000,
        |              "displayAs": "50,000.00"
        |            },
        |            "1": {
        |              "value": 2000,
        |              "displayAs": "2,000.00"
        |            }
        |          }
        |        },
        |        "transferIncludedAsset": {
        |          "0": {
        |            "0": false,
        |            "1": false
        |          },
        |          "1": {
        |            "0": true,
        |            "1": false
        |          },
        |          "2": {
        |            "0": true,
        |            "1": false
        |          },
        |          "3": {
        |            "0": true,
        |            "1": false
        |          },
        |          "4": {
        |            "0": true,
        |            "1": false
        |          },
        |          "5": {
        |            "0": true,
        |            "1": false
        |          }
        |        },
        |        "transferSchemeType": {
        |          "0": {
        |            "0": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q123456"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q123456"
        |            }
        |          },
        |          "1": {
        |            "0": {
        |              "key": "registeredPS",
        |              "value": "88390774ZZ"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q654321"
        |            }
        |          },
        |          "2": {
        |            "0": {
        |              "key": "registeredPS",
        |              "value": "88390774ZZ"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q654321"
        |            }
        |          },
        |          "3": {
        |            "0": {
        |              "key": "registeredPS",
        |              "value": "88390774ZZ"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q654321"
        |            }
        |          },
        |          "4": {
        |            "0": {
        |              "key": "registeredPS",
        |              "value": "88790774ZZ"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q654321"
        |            }
        |          },
        |          "5": {
        |            "0": {
        |              "key": "registeredPS",
        |              "value": "88390774ZZ"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q654321"
        |            }
        |          }
        |        },
        |        "reportAnotherTransferIn": {
        |          "0": {
        |            "0": false,
        |            "1": false
        |          },
        |          "1": {
        |            "0": false,
        |            "1": false
        |          },
        |          "2": {
        |            "0": false,
        |            "1": false
        |          },
        |          "3": {
        |            "0": false,
        |            "1": false
        |          },
        |          "4": {
        |            "0": false,
        |            "1": false
        |          },
        |          "5": {
        |            "0": false,
        |            "1": false
        |          }
        |        }
        |      },
        |      "memberTransfersOut": {
        |        "schemeName": {
        |          "0": {
        |            "0": "The Golden Egg Scheme",
        |            "1": "The Golden Egg Scheme"
        |          },
        |          "1": {
        |            "0": "Dodgy Pensions Ltd",
        |            "1": "My back pocket Pension Scheme"
        |          },
        |          "2": {
        |            "0": "Dodgy Pensions Ltd",
        |            "1": "My back pocket Pension Scheme"
        |          },
        |          "3": {
        |            "0": "Dodgy Pensions Ltd",
        |            "1": "My back pocket Pension Scheme"
        |          },
        |          "4": {
        |            "0": "Trial Pensions Ltd",
        |            "1": "Home User Pension Scheme"
        |          },
        |          "5": {
        |            "0": "Dodgy Pensions Ltd",
        |            "1": "My back pocket Pension Scheme"
        |          }
        |        },
        |        "dateOfTransfer": {
        |          "0": {
        |            "0": "2022-09-30",
        |            "1": "2022-12-20"
        |          },
        |          "1": {
        |            "0": "2022-05-30",
        |            "1": "2022-07-31"
        |          },
        |          "2": {
        |            "0": "2022-05-30",
        |            "1": "2022-07-31"
        |          },
        |          "3": {
        |            "0": "2022-05-30",
        |            "1": "2022-07-31"
        |          },
        |          "4": {
        |            "0": "2022-05-30",
        |            "1": "2022-07-31"
        |          },
        |          "5": {
        |            "0": "2022-05-30",
        |            "1": "2022-07-31"
        |          }
        |        },
        |        "transferSchemeType": {
        |          "0": {
        |            "0": {
        |              "key": "registeredPS",
        |              "value": "76509173AA"
        |            },
        |            "1": {
        |              "key": "registeredPS",
        |              "value": "76509173AB"
        |            }
        |          },
        |          "1": {
        |            "0": {
        |              "key": "other",
        |              "value": "Unknown identifier"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q000002"
        |            }
        |          },
        |          "2": {
        |            "0": {
        |              "key": "other",
        |              "value": "Unknown identifier"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q000002"
        |            }
        |          },
        |          "3": {
        |            "0": {
        |              "key": "other",
        |              "value": "Unknown identifier"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q000002"
        |            }
        |          },
        |          "4": {
        |            "0": {
        |              "key": "other",
        |              "value": "Unknown Pensioner"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q000002"
        |            }
        |          },
        |          "5": {
        |            "0": {
        |              "key": "other",
        |              "value": "Unknown identifier"
        |            },
        |            "1": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q000002"
        |            }
        |          }
        |        }
        |      },
        |      "totalMemberContribution": {
        |        "0": {
        |          "value": 30003,
        |          "displayAs": "30,003.00"
        |        },
        |        "1": {
        |          "value": 20000,
        |          "displayAs": "20,000.00"
        |        },
        |        "2": {
        |          "value": 20000,
        |          "displayAs": "20,000.00"
        |        },
        |        "3": {
        |          "value": 20000,
        |          "displayAs": "20,000.00"
        |        },
        |        "4": {
        |          "value": 200000,
        |          "displayAs": "200,000.00"
        |        },
        |        "5": {
        |          "value": 20000,
        |          "displayAs": "20,000.00"
        |        }
        |      },
        |      "memberLumpSumReceived": {
        |        "0": {
        |          "lumpSumAmount": {
        |            "value": 1000,
        |            "displayAs": "1,000.00"
        |          },
        |          "designatedPensionAmount": {
        |            "value": 100000,
        |            "displayAs": "100,000.00"
        |          }
        |        }
        |      },
        |      "memberPensionSurrender": {
        |        "surrenderedBenefitsSectionCompleted": {
        |          "0": {}
        |        },
        |        "totalSurrendered": {
        |          "0": {
        |            "value": 1000,
        |            "displayAs": "1,000.00"
        |          }
        |        },
        |        "dateOfSurrender": {
        |          "0": "2022-12-19"
        |        },
        |        "surrenderReason": {
        |          "0": "ABC"
        |        }
        |      },
        |      "pensionAmountReceived": {
        |        "0": {
        |          "value": 12000,
        |          "displayAs": "12,000.00"
        |        },
        |        "1": {
        |          "value": 0,
        |          "displayAs": "0.00"
        |        },
        |        "2": {
        |          "value": 0,
        |          "displayAs": "0.00"
        |        },
        |        "3": {
        |          "value": 0,
        |          "displayAs": "0.00"
        |        },
        |        "4": {
        |          "value": 0,
        |          "displayAs": "0.00"
        |        },
        |        "5": {
        |          "value": 0,
        |          "displayAs": "0.00"
        |        }
        |      },
        |      "memberPSRVersion": {
        |        "0": "003",
        |        "1": "002",
        |        "2": "004",
        |        "3": "003",
        |        "4": "004",
        |        "5": "004"
        |      }
        |    },
        |    "memberContributionMade": true,
        |    "lumpSumReceived": true,
        |    "pensionReceived": true,
        |    "employerContributionMade": true,
        |    "schemeReceivedTransferIn": true,
        |    "schemeMadeTransferOut": true,
        |    "surrenderMade": true,
        |    "soft-deleted": [
        |      {
        |        "memberPSRVersion": "004",
        |        "memberDetails": {
        |          "firstName": "Three",
        |          "lastName": "Last",
        |          "reasonNoNINO": "Could not find it on record.",
        |          "dateOfBirth": "1940-10-31"
        |        },
        |        "employerContributions": [
        |          {
        |            "employerName": "Sofa Inc.",
        |            "employerType": {
        |              "employerType": "Other",
        |              "value": "Found it down back of my sofa"
        |            },
        |            "totalTransferValue": 10000
        |          },
        |          {
        |            "employerName": "UK Company XYZ Ltd.",
        |            "employerType": {
        |              "employerType": "UKCompany",
        |              "value": "CC123456"
        |            },
        |            "totalTransferValue": 10000
        |          }
        |        ],
        |        "transfersIn": [
        |          {
        |            "schemeName": "Golden Years Pension Scheme",
        |            "dateOfTransfer": "2022-12-02",
        |            "transferSchemeType": {
        |              "key": "registeredPS",
        |              "value": "88390774ZZ"
        |            },
        |            "transferValue": 50000,
        |            "transferIncludedAsset": true
        |          },
        |          {
        |            "schemeName": "Golden Goose Egg Laying Scheme",
        |            "dateOfTransfer": "2022-10-30",
        |            "transferSchemeType": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q654321"
        |            },
        |            "transferValue": 2000,
        |            "transferIncludedAsset": false
        |          }
        |        ],
        |        "transfersOut": [
        |          {
        |            "schemeName": "Dodgy Pensions Ltd",
        |            "dateOfTransfer": "2022-05-30",
        |            "transferSchemeType": {
        |              "key": "other",
        |              "value": "Unknown identifier"
        |            }
        |          },
        |          {
        |            "schemeName": "My back pocket Pension Scheme",
        |            "dateOfTransfer": "2022-07-31",
        |            "transferSchemeType": {
        |              "key": "qualifyingRecognisedOverseasPS",
        |              "value": "Q000002"
        |            }
        |          }
        |        ],
        |        "totalMemberContribution": {
        |          "value": 20000,
        |          "displayAs": "20,000.00"
        |        },
        |        "totalAmountPensionPaymentsPage": {
        |          "value": 0,
        |          "displayAs": "0.00"
        |        }
        |      }
        |    ]
        |  }
        |}
        |""".stripMargin)

  val cleanResultJsValue: JsValue = Json.parse("""
      |{
      |  "current": "dummy-current-data",
      |  "membersPayments": {
      |    "memberDetails": {
      |      "membersDetailsChecked": false,
      |      "personalDetails": {
      |        "nameDob": {
      |          "0": {
      |            "firstName": "Ferdinand",
      |            "lastName": "Bull",
      |            "dob": "1960-05-31"
      |          },
      |          "1": {
      |            "firstName": "Johnny",
      |            "lastName": "Quicke",
      |            "dob": "1940-10-31"
      |          },
      |          "2": {
      |            "firstName": "Foury",
      |            "lastName": "Lasty",
      |            "dob": "1940-10-31"
      |          },
      |          "3": {
      |            "firstName": "Five",
      |            "lastName": "Last",
      |            "dob": "1940-10-31"
      |          },
      |          "4": {
      |            "firstName": "Six",
      |            "lastName": "User",
      |            "dob": "1949-12-31"
      |          },
      |          "5": {
      |            "firstName": "NewUser",
      |            "lastName": "LastName",
      |            "dob": "1940-10-31"
      |          }
      |        },
      |        "nationalInsuranceNumber": {
      |          "0": true,
      |          "1": false,
      |          "2": false,
      |          "3": false,
      |          "4": false,
      |          "5": false
      |        },
      |        "nino": {
      |          "0": "EB103145A"
      |        },
      |        "memberDetailsSectionCompleted": {
      |          "0": {},
      |          "1": {},
      |          "2": {},
      |          "3": {},
      |          "4": {},
      |          "5": {}
      |        },
      |        "memberStatus": {
      |          "0": "New",
      |          "1": "New",
      |          "2": "New",
      |          "3": "New",
      |          "4": "New",
      |          "5": "New"
      |        },
      |        "noNINO": {
      |          "1": "Could not find it on record.",
      |          "2": "Changed",
      |          "3": "Could not find it on record.",
      |          "4": "Could not find it on record.",
      |          "5": "Changed"
      |        },
      |        "safeToHardDelete": {
      |          "0": {},
      |          "1": {},
      |          "2": {},
      |          "3": {},
      |          "4": {},
      |          "5": {}
      |        }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
}
