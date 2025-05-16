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
import prepop.OtherAssetsPrePopulationProcessorSpec._
import models.UserAnswers.SensitiveJsObject
import controllers.TestValues
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._

import scala.util.Success

class OtherAssetsPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new OtherAssetsPrePopulationProcessor()

  "OtherAssetsPrePopulationProcessor" - {

    "clean" - {
      "should clear all optional fields, all Other Asset Disposal fields, and all fields for any fully disposed Other Assets" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val baseUa = emptyUserAnswers.copy(data = SensitiveJsObject(someDisposalsData.as[JsObject]))
        val result = processor.clean(
          baseUA = baseUa,
          currentUA = currentUa
        )(srn)

        result.get.data.decryptedValue mustBe cleanedSomeDisposalsData.as[JsObject]
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanedSomeDisposalsData.as[JsObject]))
        )
      }

      "should clear all optional fields when no Other Asset Disposals are present" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(noDisposalsData.as[JsObject])),
          currentUA = currentUa
        )(srn)

        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanedNoDisposalsData.as[JsObject]))
        )
      }
    }
  }
}

object OtherAssetsPrePopulationProcessorSpec {

  val someDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "assets": {
                 |    "otherAssets": {
                 |      "otherAssetsHeld": true,
                 |      "recordVersion": "004",
                 |      "otherAssetTransactions": {
                 |        "assetDescription": {
                 |          "0": "Bag of matches",
                 |          "1": "100kg Gold bars"
                 |        },
                 |        "methodOfHolding": {
                 |          "0": "02",
                 |          "1": "03"
                 |        },
                 |        "costOfAsset": {
                 |          "0": {
                 |            "value": 100000,
                 |            "displayAs": "100,000.00"
                 |          },
                 |          "1": {
                 |            "value": 2400000,
                 |            "displayAs": "2,400,000.00"
                 |          }
                 |        },
                 |        "movableSchedule29A": {
                 |          "0": false,
                 |          "1": false
                 |        },
                 |        "totalIncomeOrReceipts": {
                 |          "0": {
                 |            "value": 0,
                 |            "displayAs": "0.00"
                 |          },
                 |          "1": {
                 |            "value": 0,
                 |            "displayAs": "0.00"
                 |          }
                 |        },
                 |        "dateOfAcqOrContrib": {
                 |          "0": "2022-09-30"
                 |        },
                 |        "supportedByIndepValuation": {
                 |          "0": false
                 |        },
                 |        "acquiredFrom": {
                 |          "connectedPartyStatus": {
                 |            "0": true
                 |          }
                 |        },
                 |        "acquiredFromType": {
                 |          "sellerIdentityType": {
                 |            "identityTypes": {
                 |              "0": "ukCompany"
                 |            },
                 |            "crn": {
                 |              "0": {
                 |                "yes": "TS325528"
                 |              }
                 |            }
                 |          }
                 |        },
                 |        "companyNameOfOtherAssetSeller": {
                 |          "0": "Willy Den Match Co."
                 |        },
                 |        "otherAssetsCompleted": {
                 |          "0": {},
                 |          "1": {}
                 |        },
                 |        "otherAssetsDisposalCompleted": {
                 |          "0": {
                 |            "0": {
                 |              "status": "JourneyCompleted"
                 |            }
                 |          }
                 |        },
                 |        "assetsDisposed": {
                 |          "methodOfDisposal": {
                 |            "0": {
                 |              "0": {
                 |                "key": "Sold"
                 |              }
                 |            }
                 |          },
                 |          "anyPartAssetStillHeld": {
                 |            "0": {
                 |              "0": false
                 |            }
                 |          },
                 |          "purchaserType": {
                 |            "0": {
                 |              "0": "other"
                 |            }
                 |          },
                 |          "dateSold": {
                 |            "0": {
                 |              "0": "2022-12-30"
                 |            }
                 |          },
                 |          "totalAmountReceived": {
                 |            "0": {
                 |              "0": {
                 |                "value": 150000,
                 |                "displayAs": "150,000.00"
                 |              }
                 |            }
                 |          },
                 |          "connectedPartyStatus": {
                 |            "0": {
                 |              "0": false
                 |            }
                 |          },
                 |          "supportedByIndepValuation": {
                 |            "0": {
                 |              "0": true
                 |            }
                 |          },
                 |          "otherDescription": {
                 |            "0": {
                 |              "0": {
                 |                "name": "Super Express Ltd.",
                 |                "description": "Native purchaser"
                 |              }
                 |            }
                 |          }
                 |        }
                 |      }
                 |    }
                 |  },
                 |  "otherAssetsListPage": true,
                 |  "otherAssetsDisposal": true,
                 |  "otherAssetsDisposalJourneyCompleted": {}
                 |}
                 |""".stripMargin)

  val cleanedSomeDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "assets": {
                 |    "otherAssets": {
                 |    "otherAssetsPrePopulated":{"1":false},
                 |      "otherAssetTransactions": {
                 |        "assetDescription": {
                 |          "1": "100kg Gold bars"
                 |        },
                 |        "methodOfHolding": {
                 |          "1": "03"
                 |        },
                 |        "costOfAsset": {
                 |          "1": {
                 |            "value": 2400000,
                 |            "displayAs": "2,400,000.00"
                 |          }
                 |        },
                 |        "dateOfAcqOrContrib": {},
                 |        "supportedByIndepValuation": {},
                 |        "acquiredFrom": {
                 |          "connectedPartyStatus": {}
                 |        },
                 |        "acquiredFromType": {
                 |          "sellerIdentityType": {
                 |            "identityTypes": {},
                 |            "crn": {}
                 |          }
                 |        },
                 |        "companyNameOfOtherAssetSeller": {},
                 |        "otherAssetsCompleted": {
                 |          "1": {}
                 |        }
                 |      }
                 |    }
                 |  },
                 |  "current": "dummy-current-data",
                 |  "otherAssetsProgress": {
                 |    "1": { "status": "JourneyCompleted" }
                 |  }
                 |}
                 |""".stripMargin)

  val noDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "assets": {
                 |    "otherAssets": {
                 |      "otherAssetsHeld": true,
                 |      "recordVersion": "004",
                 |      "otherAssetTransactions": {
                 |        "assetDescription": {
                 |          "0": "Bag of matches",
                 |          "1": "100kg Gold bars"
                 |        },
                 |        "methodOfHolding": {
                 |          "0": "02",
                 |          "1": "03"
                 |        },
                 |        "costOfAsset": {
                 |          "0": {
                 |            "value": 100000,
                 |            "displayAs": "100,000.00"
                 |          },
                 |          "1": {
                 |            "value": 2400000,
                 |            "displayAs": "2,400,000.00"
                 |          }
                 |        },
                 |        "movableSchedule29A": {
                 |          "0": false,
                 |          "1": false
                 |        },
                 |        "totalIncomeOrReceipts": {
                 |          "0": {
                 |            "value": 0,
                 |            "displayAs": "0.00"
                 |          },
                 |          "1": {
                 |            "value": 0,
                 |            "displayAs": "0.00"
                 |          }
                 |        },
                 |        "dateOfAcqOrContrib": {
                 |          "0": "2022-09-30"
                 |        },
                 |        "supportedByIndepValuation": {
                 |          "0": false
                 |        },
                 |        "acquiredFrom": {
                 |          "connectedPartyStatus": {
                 |            "0": true
                 |          }
                 |        },
                 |        "acquiredFromType": {
                 |          "sellerIdentityType": {
                 |            "identityTypes": {
                 |              "0": "ukCompany"
                 |            },
                 |            "crn": {
                 |              "0": {
                 |                "yes": "TS325528"
                 |              }
                 |            }
                 |          }
                 |        },
                 |        "companyNameOfOtherAssetSeller": {
                 |          "0": "Willy Den Match Co."
                 |        },
                 |        "otherAssetsCompleted": {
                 |          "0": {},
                 |          "1": {}
                 |        }
                 |      }
                 |    }
                 |  },
                 |  "otherAssetsListPage": true
                 |}
                 |""".stripMargin)

  val cleanedNoDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "assets": {
                 |    "otherAssets": {
                 |    "otherAssetsPrePopulated":{"0":false,"1":false},
                 |      "otherAssetTransactions": {
                 |        "assetDescription": {
                 |          "0": "Bag of matches",
                 |          "1": "100kg Gold bars"
                 |        },
                 |        "methodOfHolding": {
                 |          "0": "02",
                 |          "1": "03"
                 |        },
                 |        "costOfAsset": {
                 |          "0": {
                 |            "value": 100000,
                 |            "displayAs": "100,000.00"
                 |          },
                 |          "1": {
                 |            "value": 2400000,
                 |            "displayAs": "2,400,000.00"
                 |          }
                 |        },
                 |        "dateOfAcqOrContrib": {
                 |          "0": "2022-09-30"
                 |        },
                 |        "supportedByIndepValuation": {
                 |          "0": false
                 |        },
                 |        "acquiredFrom": {
                 |          "connectedPartyStatus": {
                 |            "0": true
                 |          }
                 |        },
                 |        "acquiredFromType": {
                 |          "sellerIdentityType": {
                 |            "identityTypes": {
                 |              "0": "ukCompany"
                 |            },
                 |            "crn": {
                 |              "0": {
                 |                "yes": "TS325528"
                 |              }
                 |            }
                 |          }
                 |        },
                 |        "companyNameOfOtherAssetSeller": {
                 |          "0": "Willy Den Match Co."
                 |        },
                 |        "otherAssetsCompleted": {
                 |          "0": {},
                 |          "1": {}
                 |        }
                 |      }
                 |    }
                 |  },
                 |  "current": "dummy-current-data",
                 |  "otherAssetsProgress": {
                 |    "0": { "status": "JourneyCompleted" },
                 |    "1": { "status": "JourneyCompleted" }
                 |  }
                 |}
                 |""".stripMargin)
}
