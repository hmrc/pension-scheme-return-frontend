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
import prepop.SharesPrePopulationProcessorSpec._
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._

import scala.util.Success

class SharesPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new SharesPrePopulationProcessor()

  "SharesPrePopulationProcessor" - {

    "clean" - {
      "should cleanup the shares details from baseReturn and merge it onto currentUA when disposal shares exist" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(baseReturnWithDisposalsJsValue.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanResultAfterDisposalsRemovedJsValue.as[JsObject]))
        )
      }

      "should cleanup the shares details from baseReturn and merge it onto currentUA when disposal shares not exist" in {
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

object SharesPrePopulationProcessorSpec {

  val baseReturnWithDisposalsJsValue: JsValue =
    Json.parse("""
                 |{
                 |  "shares" : {
                 |    "recordVersion" : "005",
                 |    "shareTransactions" : {
                 |      "typeOfSharesHeld" : {
                 |        "0" : "01",
                 |        "1" : "03",
                 |        "2" : "03"
                 |      },
                 |      "heldSharesTransaction" : {
                 |        "methodOfHolding" : {
                 |          "0" : "01",
                 |          "1" : "01",
                 |          "2" : "02"
                 |        },
                 |        "dateOfAcqOrContrib" : {
                 |          "0" : "2022-10-29",
                 |          "1" : "2023-02-23",
                 |          "2" : "2023-02-23"
                 |        },
                 |        "totalShares" : {
                 |          "0" : 200,
                 |          "1" : 10000,
                 |          "2" : 1000
                 |        },
                 |        "acquiredFromType" : {
                 |          "sellerIdentityType" : {
                 |            "identityTypes" : {
                 |              "0" : "individual",
                 |              "1" : "ukPartnership",
                 |              "2" : "ukCompany"
                 |            },
                 |            "utr" : {
                 |              "1" : {
                 |                "yes" : "1234567890"
                 |              }
                 |            },
                 |            "crn" : {
                 |              "2" : {
                 |                "yes" : "0000123456"
                 |              }
                 |            }
                 |          }
                 |        },
                 |        "individualSellerName" : {
                 |          "0" : "Joe Bloggs"
                 |        },
                 |        "individualSellerNINumber" : {
                 |          "0" : {
                 |            "yes" : "JE113176A"
                 |          }
                 |        },
                 |        "connectedPartyStatus" : {
                 |          "0" : false,
                 |          "1" : false,
                 |          "2" : false
                 |        },
                 |        "costOfShares" : {
                 |          "0" : {
                 |            "value" : 10000,
                 |            "displayAs" : "10,000.00"
                 |          },
                 |          "1" : {
                 |            "value" : 50000,
                 |            "displayAs" : "50,000.00"
                 |          },
                 |          "2" : {
                 |            "value" : 120220.34,
                 |            "displayAs" : "120,220.34"
                 |          }
                 |        },
                 |        "totalAssetValue" : {
                 |          "0" : {
                 |            "value" : 2000,
                 |            "displayAs" : "2,000.00"
                 |          },
                 |          "1" : {
                 |            "value" : 40000,
                 |            "displayAs" : "40,000.00"
                 |          },
                 |          "2" : {
                 |            "value" : 10000,
                 |            "displayAs" : "10,000.00"
                 |          }
                 |        },
                 |        "acquiredFrom" : {
                 |          "partnershipSellerName" : {
                 |            "1" : "Platinum Investments Ltd."
                 |          }
                 |        },
                 |        "companyNameOfSharesSeller" : {
                 |          "2" : "Investec Inc."
                 |        }
                 |      },
                 |      "shareIdentification" : {
                 |        "nameOfSharesCompany" : {
                 |          "0" : "Samsung Inc.",
                 |          "1" : "Orange Computers Inc.",
                 |          "2" : "Connected Party Inc."
                 |        },
                 |        "crnNumber" : {
                 |          "0" : {
                 |            "no" : "Not able to locate Company on Companies House yet still"
                 |          },
                 |          "1" : {
                 |            "yes" : "LP299157"
                 |          },
                 |          "2" : {
                 |            "yes" : "LP289157"
                 |          }
                 |        },
                 |        "classOfShares" : {
                 |          "0" : "Ordinary Shares",
                 |          "1" : "Preferred Shares",
                 |          "2" : "Convertible Preference Shares"
                 |        }
                 |      },
                 |      "supportedByIndepValuation" : {
                 |        "0" : true,
                 |        "1" : true,
                 |        "2" : true
                 |      },
                 |      "sharesCompleted" : {
                 |        "0" : { },
                 |        "1" : { },
                 |        "2" : { }
                 |      },
                 |      "disposedSharesTransaction" : {
                 |        "sharesDisposalCompleted" : {
                 |          "0" : {
                 |            "0" : {
                 |              "status" : "JourneyCompleted"
                 |            },
                 |            "1" : {
                 |              "status" : "JourneyCompleted"
                 |            }
                 |          },
                 |          "1" : {
                 |            "0" : {
                 |              "status" : "JourneyCompleted"
                 |            },
                 |            "1" : {
                 |              "status" : "JourneyCompleted"
                 |            }
                 |          },
                 |          "2" : {
                 |            "0" : {
                 |              "status" : "JourneyCompleted"
                 |            }
                 |          }
                 |        },
                 |        "methodOfDisposal" : {
                 |          "0" : {
                 |            "0" : {
                 |              "key" : "Sold"
                 |            },
                 |            "1" : {
                 |              "key" : "Redeemed"
                 |            }
                 |          },
                 |          "1" : {
                 |            "0" : {
                 |              "key" : "Sold"
                 |            },
                 |            "1" : {
                 |              "key" : "Redeemed"
                 |            }
                 |          },
                 |          "2" : {
                 |            "0" : {
                 |              "key" : "Sold"
                 |            }
                 |          }
                 |        },
                 |        "totalSharesNowHeld" : {
                 |          "0" : {
                 |            "0" : 150,
                 |            "1" : 100
                 |          },
                 |          "1" : {
                 |            "0" : 8000,
                 |            "1" : 0
                 |          },
                 |          "2" : {
                 |            "0" : 400
                 |          }
                 |        },
                 |        "salesQuestions" : {
                 |          "dateOfSale" : {
                 |            "0" : {
                 |              "0" : "2023-02-16"
                 |            },
                 |            "1" : {
                 |              "0" : "2022-10-31"
                 |            },
                 |            "2" : {
                 |              "0" : "2022-12-31"
                 |            }
                 |          },
                 |          "noOfSharesSold" : {
                 |            "0" : {
                 |              "0" : 50
                 |            },
                 |            "1" : {
                 |              "0" : 1100
                 |            },
                 |            "2" : {
                 |              "0" : 200
                 |            }
                 |          },
                 |          "amountReceived" : {
                 |            "0" : {
                 |              "0" : {
                 |                "value" : 8000,
                 |                "displayAs" : "8,000.00"
                 |              }
                 |            },
                 |            "1" : {
                 |              "0" : {
                 |                "value" : 30000,
                 |                "displayAs" : "30,000.00"
                 |              }
                 |            },
                 |            "2" : {
                 |              "0" : {
                 |                "value" : 52000,
                 |                "displayAs" : "52,000.00"
                 |              }
                 |            }
                 |          },
                 |          "purchaserType" : {
                 |            "0" : {
                 |              "0" : "individual"
                 |            },
                 |            "1" : {
                 |              "0" : "individual"
                 |            },
                 |            "2" : {
                 |              "0" : "individual"
                 |            }
                 |          },
                 |          "individualBuyerName" : {
                 |            "0" : {
                 |              "0" : "Shareacquires Inc."
                 |            },
                 |            "1" : {
                 |              "0" : "Share Acquisitions Inc."
                 |            },
                 |            "2" : {
                 |              "0" : "James Smithsonian"
                 |            }
                 |          },
                 |          "connectedPartyStatus" : {
                 |            "0" : {
                 |              "0" : false
                 |            },
                 |            "1" : {
                 |              "0" : false
                 |            },
                 |            "2" : {
                 |              "0" : true
                 |            }
                 |          }
                 |        },
                 |        "individualBuyerNinoNumber" : {
                 |          "0" : {
                 |            "0" : {
                 |              "yes" : "SX423456A"
                 |            }
                 |          },
                 |          "1" : {
                 |            "0" : {
                 |              "yes" : "JJ507888A"
                 |            }
                 |          },
                 |          "2" : {
                 |            "0" : {
                 |              "yes" : "JE443364A"
                 |            }
                 |          }
                 |        },
                 |        "supportedByIndepValuation" : {
                 |          "0" : {
                 |            "0" : true
                 |          },
                 |          "1" : {
                 |            "0" : true
                 |          },
                 |          "2" : {
                 |            "0" : true
                 |          }
                 |        },
                 |        "redemptionQuestions" : {
                 |          "dateOfRedemption" : {
                 |            "0" : {
                 |              "1" : "2023-03-06"
                 |            },
                 |            "1" : {
                 |              "1" : "2022-12-20"
                 |            }
                 |          },
                 |          "noOfSharesRedeemed" : {
                 |            "0" : {
                 |              "1" : 50
                 |            },
                 |            "1" : {
                 |              "1" : 900
                 |            }
                 |          },
                 |          "amountReceived" : {
                 |            "0" : {
                 |              "1" : {
                 |                "value" : 7600,
                 |                "displayAs" : "7,600.00"
                 |              }
                 |            },
                 |            "1" : {
                 |              "1" : {
                 |                "value" : 27005.78,
                 |                "displayAs" : "27,005.78"
                 |              }
                 |            }
                 |          }
                 |        }
                 |      }
                 |    },
                 |    "didSchemeDisposeAnyShares" : true
                 |  }
                 |}
                 |""".stripMargin)

  val baseReturnWithoutDisposalsJsValue: JsValue =
    Json.parse("""
        |{
        |  "shares" : {
        |    "shareTransactions" : {
        |      "typeOfSharesHeld" : {
        |        "0" : "01",
        |        "1" : "03",
        |        "2" : "03"
        |      },
        |      "heldSharesTransaction" : {
        |        "methodOfHolding" : {
        |          "0" : "01",
        |          "1" : "01",
        |          "2" : "02"
        |        },
        |        "dateOfAcqOrContrib" : {
        |          "0" : "2022-10-29",
        |          "1" : "2023-02-23",
        |          "2" : "2023-02-23"
        |        },
        |        "totalShares" : {
        |          "0" : 200,
        |          "1" : 10000,
        |          "2" : 1000
        |        },
        |        "acquiredFromType" : {
        |          "sellerIdentityType" : {
        |            "identityTypes" : {
        |              "0" : "individual",
        |              "1" : "ukPartnership",
        |              "2" : "ukCompany"
        |            },
        |            "utr" : {
        |              "1" : {
        |                "yes" : "1234567890"
        |              }
        |            },
        |            "crn" : {
        |              "2" : {
        |                "yes" : "0000123456"
        |              }
        |            }
        |          }
        |        },
        |        "individualSellerName" : {
        |          "0" : "Joe Bloggs"
        |        },
        |        "individualSellerNINumber" : {
        |          "0" : {
        |            "yes" : "JE113176A"
        |          }
        |        },
        |        "connectedPartyStatus" : {
        |          "0" : false,
        |          "1" : false,
        |          "2" : false
        |        },
        |        "costOfShares" : {
        |          "0" : {
        |            "value" : 10000,
        |            "displayAs" : "10,000.00"
        |          },
        |          "1" : {
        |            "value" : 50000,
        |            "displayAs" : "50,000.00"
        |          },
        |          "2" : {
        |            "value" : 120220.34,
        |            "displayAs" : "120,220.34"
        |          }
        |        },
        |        "totalAssetValue" : {
        |          "0" : {
        |            "value" : 2000,
        |            "displayAs" : "2,000.00"
        |          },
        |          "1" : {
        |            "value" : 40000,
        |            "displayAs" : "40,000.00"
        |          },
        |          "2" : {
        |            "value" : 10000,
        |            "displayAs" : "10,000.00"
        |          }
        |        },
        |        "acquiredFrom" : {
        |          "partnershipSellerName" : {
        |            "1" : "Platinum Investments Ltd."
        |          }
        |        },
        |        "companyNameOfSharesSeller" : {
        |          "2" : "Investec Inc."
        |        }
        |      },
        |      "shareIdentification" : {
        |        "nameOfSharesCompany" : {
        |          "0" : "Samsung Inc.",
        |          "1" : "Orange Computers Inc.",
        |          "2" : "Connected Party Inc."
        |        },
        |        "crnNumber" : {
        |          "0" : {
        |            "no" : "Not able to locate Company on Companies House yet still"
        |          },
        |          "1" : {
        |            "yes" : "LP299157"
        |          },
        |          "2" : {
        |            "yes" : "LP289157"
        |          }
        |        },
        |        "classOfShares" : {
        |          "0" : "Ordinary Shares",
        |          "1" : "Preferred Shares",
        |          "2" : "Convertible Preference Shares"
        |        }
        |      },
        |      "supportedByIndepValuation" : {
        |        "0" : true,
        |        "1" : true,
        |        "2" : true
        |      },
        |      "sharesCompleted" : {
        |        "0" : { },
        |        "1" : { },
        |        "2" : { }
        |      }
        |    }
        |  }
        |}
        |""".stripMargin)

  val cleanResultAfterDisposalsRemovedJsValue: JsValue =
    Json.parse("""
        |{
        |  "current": "dummy-current-data",
        |  "shares" : {
        |    "shareTransactions" : {
        |      "typeOfSharesHeld" : {
        |        "0" : "01",
        |        "2" : "03"
        |      },
        |      "heldSharesTransaction" : {
        |        "methodOfHolding" : {
        |          "0" : "01",
        |          "2" : "02"
        |        },
        |        "dateOfAcqOrContrib" : {
        |          "0" : "2022-10-29",
        |          "2" : "2023-02-23"
        |        },
        |        "totalShares" : {
        |          "0" : 200,
        |          "2" : 1000
        |        },
        |        "acquiredFromType" : {
        |          "sellerIdentityType" : {
        |            "identityTypes" : {
        |              "0" : "individual",
        |              "2" : "ukCompany"
        |            },
        |            "utr" : { },
        |            "crn" : {
        |              "2" : {
        |                "yes" : "0000123456"
        |              }
        |            }
        |          }
        |        },
        |        "individualSellerName" : {
        |          "0" : "Joe Bloggs"
        |        },
        |        "individualSellerNINumber" : {
        |          "0" : {
        |            "yes" : "JE113176A"
        |          }
        |        },
        |        "connectedPartyStatus" : {
        |          "0" : false,
        |          "2" : false
        |        },
        |        "costOfShares" : {
        |          "0" : {
        |            "value" : 10000,
        |            "displayAs" : "10,000.00"
        |          },
        |          "2" : {
        |            "value" : 120220.34,
        |            "displayAs" : "120,220.34"
        |          }
        |        },
        |        "totalAssetValue" : {
        |          "0" : {
        |            "value" : 2000,
        |            "displayAs" : "2,000.00"
        |          },
        |          "2" : {
        |            "value" : 10000,
        |            "displayAs" : "10,000.00"
        |          }
        |        },
        |        "acquiredFrom" : {
        |          "partnershipSellerName" : { }
        |        },
        |        "companyNameOfSharesSeller" : {
        |          "2" : "Investec Inc."
        |        }
        |      },
        |      "shareIdentification" : {
        |        "nameOfSharesCompany" : {
        |          "0" : "Samsung Inc.",
        |          "2" : "Connected Party Inc."
        |        },
        |        "crnNumber" : {
        |          "0" : {
        |            "no" : "Not able to locate Company on Companies House yet still"
        |          },
        |          "2" : {
        |            "yes" : "LP289157"
        |          }
        |        },
        |        "classOfShares" : {
        |          "0" : "Ordinary Shares",
        |          "2" : "Convertible Preference Shares"
        |        }
        |      },
        |      "supportedByIndepValuation" : {
        |        "0" : true,
        |        "2" : true
        |      },
        |      "sharesCompleted" : {
        |        "0" : { },
        |        "2" : { }
        |      }
        |    }
        |  }
        |}
        |""".stripMargin)

  val cleanResultJsValue: JsValue =
    Json.parse("""
      |{
      |  "current": "dummy-current-data",
      |  "shares" : {
      |    "shareTransactions" : {
      |      "typeOfSharesHeld" : {
      |        "0" : "01",
      |        "1" : "03",
      |        "2" : "03"
      |      },
      |      "heldSharesTransaction" : {
      |        "methodOfHolding" : {
      |          "0" : "01",
      |          "1" : "01",
      |          "2" : "02"
      |        },
      |        "dateOfAcqOrContrib" : {
      |          "0" : "2022-10-29",
      |          "1" : "2023-02-23",
      |          "2" : "2023-02-23"
      |        },
      |        "totalShares" : {
      |          "0" : 200,
      |          "1" : 10000,
      |          "2" : 1000
      |        },
      |        "acquiredFromType" : {
      |          "sellerIdentityType" : {
      |            "identityTypes" : {
      |              "0" : "individual",
      |              "1" : "ukPartnership",
      |              "2" : "ukCompany"
      |            },
      |            "utr" : {
      |              "1" : {
      |                "yes" : "1234567890"
      |              }
      |            },
      |            "crn" : {
      |              "2" : {
      |                "yes" : "0000123456"
      |              }
      |            }
      |          }
      |        },
      |        "individualSellerName" : {
      |          "0" : "Joe Bloggs"
      |        },
      |        "individualSellerNINumber" : {
      |          "0" : {
      |            "yes" : "JE113176A"
      |          }
      |        },
      |        "connectedPartyStatus" : {
      |          "0" : false,
      |          "1" : false,
      |          "2" : false
      |        },
      |        "costOfShares" : {
      |          "0" : {
      |            "value" : 10000,
      |            "displayAs" : "10,000.00"
      |          },
      |          "1" : {
      |            "value" : 50000,
      |            "displayAs" : "50,000.00"
      |          },
      |          "2" : {
      |            "value" : 120220.34,
      |            "displayAs" : "120,220.34"
      |          }
      |        },
      |        "totalAssetValue" : {
      |          "0" : {
      |            "value" : 2000,
      |            "displayAs" : "2,000.00"
      |          },
      |          "1" : {
      |            "value" : 40000,
      |            "displayAs" : "40,000.00"
      |          },
      |          "2" : {
      |            "value" : 10000,
      |            "displayAs" : "10,000.00"
      |          }
      |        },
      |        "acquiredFrom" : {
      |          "partnershipSellerName" : {
      |            "1" : "Platinum Investments Ltd."
      |          }
      |        },
      |        "companyNameOfSharesSeller" : {
      |          "2" : "Investec Inc."
      |        }
      |      },
      |      "shareIdentification" : {
      |        "nameOfSharesCompany" : {
      |          "0" : "Samsung Inc.",
      |          "1" : "Orange Computers Inc.",
      |          "2" : "Connected Party Inc."
      |        },
      |        "crnNumber" : {
      |          "0" : {
      |            "no" : "Not able to locate Company on Companies House yet still"
      |          },
      |          "1" : {
      |            "yes" : "LP299157"
      |          },
      |          "2" : {
      |            "yes" : "LP289157"
      |          }
      |        },
      |        "classOfShares" : {
      |          "0" : "Ordinary Shares",
      |          "1" : "Preferred Shares",
      |          "2" : "Convertible Preference Shares"
      |        }
      |      },
      |      "supportedByIndepValuation" : {
      |        "0" : true,
      |        "1" : true,
      |        "2" : true
      |      },
      |      "sharesCompleted" : {
      |        "0" : { },
      |        "1" : { },
      |        "2" : { }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
}
