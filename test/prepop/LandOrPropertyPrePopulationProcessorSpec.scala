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
import prepop.LandOrPropertyPrePopulationProcessorSpec._
import models.UserAnswers.SensitiveJsObject
import controllers.TestValues
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._

import scala.util.Success

class LandOrPropertyPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new LandOrPropertyPrePopulationProcessor()

  "LandOrPropertyPrePopulationProcessor" - {

    "clean" - {
      "should cleanup the LoP details from baseReturn and merge it onto currentUA when disposal Lop exist" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(baseReturnWithDisposalsJsValue.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanResultAfterDisposalsRemovedJsValue.as[JsObject]))
        )
      }

      "should cleanup the LoP details from baseReturn and merge it onto currentUA when disposal Lop not exist" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(baseReturnWithoutDisposalsJsValue.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanResultJsValue.as[JsObject]))
        )
      }
    }
  }
}

object LandOrPropertyPrePopulationProcessorSpec {

  val baseReturnWithoutDisposalsJsValue: JsValue =
    Json.parse("""
        |{
        |  "assets": {
        |    "landOrProperty": {
        |      "landOrPropertyHeld": true,
        |      "recordVersion": "004",
        |      "landOrPropertyTransactions": {
        |        "propertyDetails": {
        |          "landOrPropertyInUK": {
        |            "0": false,
        |            "1": true,
        |            "2": false
        |          },
        |          "addressDetails": {
        |            "landOrPropertyHeld": {
        |              "0": {
        |                "id": "manual",
        |                "addressLine1": "Jason",
        |                "addressLine3": "Kadikoy",
        |                "town": "Williams",
        |                "country": "Turkey",
        |                "countryCode": "TR",
        |                "addressType": {
        |                  "_type": "models.ManualAddress"
        |                }
        |              },
        |              "1": {
        |                "id": "manual",
        |                "addressLine1": "Beyoglu",
        |                "addressLine2": "Ulker Arena",
        |                "addressLine3": "Kadikoy",
        |                "town": "Istanbul",
        |                "postCode": "GB135HG",
        |                "country": "United Kingdom",
        |                "countryCode": "GB",
        |                "addressType": {
        |                  "_type": "models.ManualAddress"
        |                }
        |              },
        |              "2": {
        |                "id": "manual",
        |                "addressLine1": "1 Hacienda Way",
        |                "addressLine3": "01055",
        |                "town": "Madrid",
        |                "country": "Spain",
        |                "countryCode": "ES",
        |                "addressType": {
        |                  "_type": "models.ManualAddress"
        |                }
        |              }
        |            }
        |          },
        |          "landRegistryTitleNumber": {
        |            "0": {
        |              "no": "Foreign property"
        |            },
        |            "1": {
        |              "yes": "LR10000102202202"
        |            },
        |            "2": {
        |              "no": "Foreign property"
        |            }
        |          }
        |        },
        |        "heldPropertyTransaction": {
        |          "methodOfHolding": {
        |            "0": "Acquisition",
        |            "1": "Contribution",
        |            "2": "Acquisition"
        |          },
        |          "totalCostOfLandOrProperty": {
        |            "0": {
        |              "value": 1000000,
        |              "displayAs": "1,000,000.00"
        |            },
        |            "1": {
        |              "value": 1000000,
        |              "displayAs": "1,000,000.00"
        |            },
        |            "2": {
        |              "value": 14000000,
        |              "displayAs": "14,000,000.00"
        |            }
        |          },
        |          "isLandOrPropertyResidential": {
        |            "0": true,
        |            "1": true,
        |            "2": false
        |          },
        |          "landOrPropertyLeased": {
        |            "0": false,
        |            "1": false,
        |            "2": true
        |          },
        |          "totalIncomeOrReceipts": {
        |            "0": {
        |              "value": 25000,
        |              "displayAs": "25,000.00"
        |            },
        |            "1": {
        |              "value": 25000,
        |              "displayAs": "25,000.00"
        |            },
        |            "2": {
        |              "value": 500000,
        |              "displayAs": "500,000.00"
        |            }
        |          },
        |          "dateOfAcquisitionOrContribution": {
        |            "0": "1953-03-28",
        |            "1": "1953-03-28",
        |            "2": "2022-12-30"
        |          },
        |          "indepValuationSupport": {
        |            "0": true,
        |            "1": false,
        |            "2": false
        |          },
        |          "propertyAcquiredFrom": {
        |            "sellerIdentityType": {
        |              "identityTypes": {
        |                "0": "individual",
        |                "1": "individual",
        |                "2": "individual"
        |              }
        |            }
        |          },
        |          "individualSellerName": {
        |            "0": "Willy Wonky Housing Estates Ltd",
        |            "1": "Willy Wonky Housing Estates Ltd.",
        |            "2": "Alpine Sussex"
        |          },
        |          "individualRecipientNinoNumber": {
        |            "0": {
        |              "yes": "SX123456A"
        |            },
        |            "1": {
        |              "yes": "SX123456A"
        |            },
        |            "2": {
        |              "yes": "SX654321A"
        |            }
        |          },
        |          "connectedPartyStatus": {
        |            "0": true,
        |            "1": true,
        |            "2": false
        |          },
        |          "leaseDetails": {
        |            "2": [
        |              "Leasee",
        |              {
        |                "value": 500000,
        |                "displayAs": "500,000.00"
        |              },
        |              "2023-01-17"
        |            ],
        |            "connectedPartyStatus": {
        |              "isLesseeConnectedParty": {
        |                "2": false
        |              }
        |            }
        |          }
        |        }
        |      },
        |      "landOrPropertyCompleted": {
        |        "0": {},
        |        "1": {},
        |        "2": {}
        |      }
        |    }
        |  }
        |}
        |""".stripMargin)

  val baseReturnWithDisposalsJsValue: JsValue =
    Json.parse("""
        |{
        |  "assets": {
        |    "landOrProperty": {
        |      "landOrPropertyHeld": true,
        |      "recordVersion": "004",
        |      "landOrPropertyTransactions": {
        |        "propertyDetails": {
        |          "landOrPropertyInUK": {
        |            "0": false,
        |            "1": true,
        |            "2": false
        |          },
        |          "addressDetails": {
        |            "landOrPropertyHeld": {
        |              "0": {
        |                "id": "manual",
        |                "addressLine1": "Jason",
        |                "addressLine3": "Kadikoy",
        |                "town": "Williams",
        |                "country": "Turkey",
        |                "countryCode": "TR",
        |                "addressType": {
        |                  "_type": "models.ManualAddress"
        |                }
        |              },
        |              "1": {
        |                "id": "manual",
        |                "addressLine1": "Beyoglu",
        |                "addressLine2": "Ulker Arena",
        |                "addressLine3": "Kadikoy",
        |                "town": "Istanbul",
        |                "postCode": "GB135HG",
        |                "country": "United Kingdom",
        |                "countryCode": "GB",
        |                "addressType": {
        |                  "_type": "models.ManualAddress"
        |                }
        |              },
        |              "2": {
        |                "id": "manual",
        |                "addressLine1": "1 Hacienda Way",
        |                "addressLine3": "01055",
        |                "town": "Madrid",
        |                "country": "Spain",
        |                "countryCode": "ES",
        |                "addressType": {
        |                  "_type": "models.ManualAddress"
        |                }
        |              }
        |            }
        |          },
        |          "landRegistryTitleNumber": {
        |            "0": {
        |              "no": "Foreign property"
        |            },
        |            "1": {
        |              "yes": "LR10000102202202"
        |            },
        |            "2": {
        |              "no": "Foreign property"
        |            }
        |          }
        |        },
        |        "heldPropertyTransaction": {
        |          "methodOfHolding": {
        |            "0": "Acquisition",
        |            "1": "Contribution",
        |            "2": "Acquisition"
        |          },
        |          "totalCostOfLandOrProperty": {
        |            "0": {
        |              "value": 1000000,
        |              "displayAs": "1,000,000.00"
        |            },
        |            "1": {
        |              "value": 1000000,
        |              "displayAs": "1,000,000.00"
        |            },
        |            "2": {
        |              "value": 14000000,
        |              "displayAs": "14,000,000.00"
        |            }
        |          },
        |          "isLandOrPropertyResidential": {
        |            "0": true,
        |            "1": true,
        |            "2": false
        |          },
        |          "landOrPropertyLeased": {
        |            "0": false,
        |            "1": false,
        |            "2": true
        |          },
        |          "totalIncomeOrReceipts": {
        |            "0": {
        |              "value": 25000,
        |              "displayAs": "25,000.00"
        |            },
        |            "1": {
        |              "value": 25000,
        |              "displayAs": "25,000.00"
        |            },
        |            "2": {
        |              "value": 500000,
        |              "displayAs": "500,000.00"
        |            }
        |          },
        |          "dateOfAcquisitionOrContribution": {
        |            "0": "1953-03-28",
        |            "1": "1953-03-28",
        |            "2": "2022-12-30"
        |          },
        |          "indepValuationSupport": {
        |            "0": true,
        |            "1": false,
        |            "2": false
        |          },
        |          "propertyAcquiredFrom": {
        |            "sellerIdentityType": {
        |              "identityTypes": {
        |                "0": "individual",
        |                "1": "individual",
        |                "2": "individual"
        |              }
        |            }
        |          },
        |          "individualSellerName": {
        |            "0": "Willy Wonky Housing Estates Ltd",
        |            "1": "Willy Wonky Housing Estates Ltd.",
        |            "2": "Alpine Sussex"
        |          },
        |          "individualRecipientNinoNumber": {
        |            "0": {
        |              "yes": "SX123456A"
        |            },
        |            "1": {
        |              "yes": "SX123456A"
        |            },
        |            "2": {
        |              "yes": "SX654321A"
        |            }
        |          },
        |          "connectedPartyStatus": {
        |            "0": true,
        |            "1": true,
        |            "2": false
        |          },
        |          "leaseDetails": {
        |            "2": [
        |              "Leasee",
        |              {
        |                "value": 500000,
        |                "displayAs": "500,000.00"
        |              },
        |              "2023-01-17"
        |            ],
        |            "connectedPartyStatus": {
        |              "isLesseeConnectedParty": {
        |                "2": false
        |              }
        |            }
        |          }
        |        },
        |        "disposedPropertyTransaction": {
        |          "disposalCompleted": {
        |            "0": {
        |              "0": {},
        |              "1": {}
        |            },
        |            "1": {
        |              "0": {}
        |            },
        |            "2": {
        |              "0": {},
        |              "1": {}
        |            }
        |          },
        |          "methodOfDisposal": {
        |            "0": {
        |              "0": {
        |                "key": "Sold"
        |              },
        |              "1": {
        |                "key": "Sold"
        |              }
        |            },
        |            "1": {
        |              "0": {
        |                "key": "Sold"
        |              }
        |            },
        |            "2": {
        |              "0": {
        |                "key": "Sold"
        |              },
        |              "1": {
        |                "key": "Sold"
        |              }
        |            }
        |          },
        |          "portionStillHeld": {
        |            "0": {
        |              "0": true,
        |              "1": false
        |            },
        |            "1": {
        |              "0": false
        |            },
        |            "2": {
        |              "0": true,
        |              "1": true
        |            }
        |          },
        |          "dateOfSale": {
        |            "0": {
        |              "0": "2021-10-19",
        |              "1": "2022-10-19"
        |            },
        |            "1": {
        |              "0": "2022-10-19"
        |            },
        |            "2": {
        |              "0": "2022-11-09",
        |              "1": "2023-01-26"
        |            }
        |          },
        |          "saleProceeds": {
        |            "0": {
        |              "0": {
        |                "value": 1500000,
        |                "displayAs": "1,500,000.00"
        |              },
        |              "1": {
        |                "value": 1550000,
        |                "displayAs": "1,550,000.00"
        |              }
        |            },
        |            "1": {
        |              "0": {
        |                "value": 1500000,
        |                "displayAs": "1,500,000.00"
        |              }
        |            },
        |            "2": {
        |              "0": {
        |                "value": 1550000,
        |                "displayAs": "1,550,000.00"
        |              },
        |              "1": {
        |                "value": 10234.56,
        |                "displayAs": "10,234.56"
        |              }
        |            }
        |          },
        |          "indepValuationSupport": {
        |            "0": {
        |              "0": false,
        |              "1": false
        |            },
        |            "1": {
        |              "0": false
        |            },
        |            "2": {
        |              "0": false,
        |              "1": false
        |            }
        |          },
        |          "purchasedLandOrProperty": {
        |            "0": {
        |              "0": "ukCompany",
        |              "1": "ukCompany"
        |            },
        |            "1": {
        |              "0": "ukCompany"
        |            },
        |            "2": {
        |              "0": "ukCompany",
        |              "1": "ukCompany"
        |            }
        |          },
        |          "nameOfPurchaser": {
        |            "0": {
        |              "0": "Spartan Enterprises Inc.",
        |              "1": "Second Purchaser."
        |            },
        |            "1": {
        |              "0": "Spartan Enterprises Inc."
        |            },
        |            "2": {
        |              "0": "Virtual Purchasers Co.",
        |              "1": "XY~=Z Company Inc."
        |            }
        |          },
        |          "idNumber": {
        |            "0": {
        |              "0": {
        |                "yes": "24896221"
        |              },
        |              "1": {
        |                "yes": "24896221"
        |              }
        |            },
        |            "1": {
        |              "0": {
        |                "yes": "24896221"
        |              }
        |            },
        |            "2": {
        |              "0": {
        |                "yes": "JE463863"
        |              },
        |              "1": {
        |                "yes": "DA576257"
        |              }
        |            }
        |          },
        |          "connectedPartyStatus": {
        |            "0": {
        |              "0": true,
        |              "1": true
        |            },
        |            "1": {
        |              "0": true
        |            },
        |            "2": {
        |              "0": true,
        |              "1": false
        |            }
        |          }
        |        }
        |      },
        |      "landOrPropertyCompleted": {
        |        "0": {},
        |        "1": {},
        |        "2": {}
        |      },
        |      "disposeAnyLandOrProperty": true
        |    }
        |  }
        |}
        |""".stripMargin)

  val cleanResultAfterDisposalsRemovedJsValue: JsValue = Json.parse("""
      |{
      |  "current": "dummy-current-data",
      |  "assets": {
      |    "landOrProperty": {
      |      "landOrPropertyTransactions": {
      |        "propertyDetails": {
      |          "landOrPropertyInUK": {
      |            "2": false
      |          },
      |          "addressDetails": {
      |            "landOrPropertyHeld": {
      |              "2": {
      |                "id": "manual",
      |                "addressLine1": "1 Hacienda Way",
      |                "addressLine3": "01055",
      |                "town": "Madrid",
      |                "country": "Spain",
      |                "countryCode": "ES",
      |                "addressType": {
      |                  "_type": "models.ManualAddress"
      |                }
      |              }
      |            }
      |          },
      |          "landRegistryTitleNumber": {
      |            "2": {
      |              "no": "Foreign property"
      |            }
      |          }
      |        },
      |        "heldPropertyTransaction": {
      |          "methodOfHolding": {
      |            "2": "Acquisition"
      |          },
      |          "totalCostOfLandOrProperty": {
      |            "2": {
      |              "value": 14000000,
      |              "displayAs": "14,000,000.00"
      |            }
      |          },
      |          "dateOfAcquisitionOrContribution": {
      |            "2": "2022-12-30"
      |          },
      |          "indepValuationSupport": {
      |            "2": false
      |          },
      |          "propertyAcquiredFrom": {
      |            "sellerIdentityType": {
      |              "identityTypes": {
      |                "2": "individual"
      |              }
      |            }
      |          },
      |          "individualSellerName": {
      |            "2": "Alpine Sussex"
      |          },
      |          "individualRecipientNinoNumber": {
      |            "2": {
      |              "yes": "SX654321A"
      |            }
      |          },
      |          "connectedPartyStatus": {
      |            "2": false
      |          }
      |        }
      |      },
      |      "landOrPropertyCompleted": {
      |        "2": {}
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)

  val cleanResultJsValue: JsValue = Json.parse("""
      |{
      |  "current": "dummy-current-data",
      |  "assets": {
      |    "landOrProperty": {
      |      "landOrPropertyTransactions": {
      |        "propertyDetails": {
      |          "landOrPropertyInUK": {
      |            "0": false,
      |            "1": true,
      |            "2": false
      |          },
      |          "addressDetails": {
      |            "landOrPropertyHeld": {
      |              "0": {
      |                "id": "manual",
      |                "addressLine1": "Jason",
      |                "addressLine3": "Kadikoy",
      |                "town": "Williams",
      |                "country": "Turkey",
      |                "countryCode": "TR",
      |                "addressType": {
      |                  "_type": "models.ManualAddress"
      |                }
      |              },
      |              "1": {
      |                "id": "manual",
      |                "addressLine1": "Beyoglu",
      |                "addressLine2": "Ulker Arena",
      |                "addressLine3": "Kadikoy",
      |                "town": "Istanbul",
      |                "postCode": "GB135HG",
      |                "country": "United Kingdom",
      |                "countryCode": "GB",
      |                "addressType": {
      |                  "_type": "models.ManualAddress"
      |                }
      |              },
      |              "2": {
      |                "id": "manual",
      |                "addressLine1": "1 Hacienda Way",
      |                "addressLine3": "01055",
      |                "town": "Madrid",
      |                "country": "Spain",
      |                "countryCode": "ES",
      |                "addressType": {
      |                  "_type": "models.ManualAddress"
      |                }
      |              }
      |            }
      |          },
      |          "landRegistryTitleNumber": {
      |            "0": {
      |              "no": "Foreign property"
      |            },
      |            "1": {
      |              "yes": "LR10000102202202"
      |            },
      |            "2": {
      |              "no": "Foreign property"
      |            }
      |          }
      |        },
      |        "heldPropertyTransaction": {
      |          "methodOfHolding": {
      |            "0": "Acquisition",
      |            "1": "Contribution",
      |            "2": "Acquisition"
      |          },
      |          "totalCostOfLandOrProperty": {
      |            "0": {
      |              "value": 1000000,
      |              "displayAs": "1,000,000.00"
      |            },
      |            "1": {
      |              "value": 1000000,
      |              "displayAs": "1,000,000.00"
      |            },
      |            "2": {
      |              "value": 14000000,
      |              "displayAs": "14,000,000.00"
      |            }
      |          },
      |          "dateOfAcquisitionOrContribution": {
      |            "0": "1953-03-28",
      |            "1": "1953-03-28",
      |            "2": "2022-12-30"
      |          },
      |          "indepValuationSupport": {
      |            "0": true,
      |            "1": false,
      |            "2": false
      |          },
      |          "propertyAcquiredFrom": {
      |            "sellerIdentityType": {
      |              "identityTypes": {
      |                "0": "individual",
      |                "1": "individual",
      |                "2": "individual"
      |              }
      |            }
      |          },
      |          "individualSellerName": {
      |            "0": "Willy Wonky Housing Estates Ltd",
      |            "1": "Willy Wonky Housing Estates Ltd.",
      |            "2": "Alpine Sussex"
      |          },
      |          "individualRecipientNinoNumber": {
      |            "0": {
      |              "yes": "SX123456A"
      |            },
      |            "1": {
      |              "yes": "SX123456A"
      |            },
      |            "2": {
      |              "yes": "SX654321A"
      |            }
      |          },
      |          "connectedPartyStatus": {
      |            "0": true,
      |            "1": true,
      |            "2": false
      |          }
      |        }
      |      },
      |      "landOrPropertyCompleted": {
      |        "0": {},
      |        "1": {},
      |        "2": {}
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
}
