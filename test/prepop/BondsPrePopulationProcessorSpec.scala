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
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._
import prepop.BondsPrePopulationProcessorSpec._

import scala.util.Success

class BondsPrePopulationProcessorSpec extends BaseSpec with TestValues {

  private val processor = new BondsPrePopulationProcessor()

  "BondsPrePopulationProcessor" - {

    "clean" - {
      "should clear all optional fields, all Bonds Disposal fields, and all fields for any fully disposed Bonds" in {
        val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
        val result = processor.clean(
          baseUA = emptyUserAnswers.copy(data = SensitiveJsObject(someDisposalsData.as[JsObject])),
          currentUA = currentUa
        )(srn)
        result.get.data.decryptedValue mustBe cleanedSomeDisposalsData.as[JsObject]
        result mustBe Success(
          currentUa.copy(data = SensitiveJsObject(cleanedSomeDisposalsData.as[JsObject]))
        )
      }

      "should clear all optional fields when no Bonds Disposals are present" in {
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

object BondsPrePopulationProcessorSpec {

  val someDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "assets": {
                 |    "bonds": {
                 |      "unregulatedOrConnectedBondsHeld": true,
                 |      "recordVersion": "001",
                 |      "bondTransactions": {
                 |        "nameOfBonds": {
                 |          "0": "first bonds - no disposals",
                 |          "1": "second bonds - fully disposed after one disposal",
                 |          "2": "third bonds - partially disposed after one disposal",
                 |          "3": "fourth bonds - fully disposed after two disposals",
                 |          "4": "fifth bonds - partially disposed after two disposals"
                 |        },
                 |        "methodOfHolding": {
                 |          "0": "01",
                 |          "1": "02",
                 |          "2": "03",
                 |          "3": "01",
                 |          "4": "02"
                 |        },
                 |        "dateOfAcqOrContrib": {
                 |          "0": "2001-01-01",
                 |          "1": "2002-02-02",
                 |          "3": "2004-04-04",
                 |          "4": "2005-05-05"
                 |        },
                 |        "costOfBonds": {
                 |          "0": {
                 |            "value": 111,
                 |            "displayAs": "111.00"
                 |          },
                 |          "1": {
                 |            "value": 222,
                 |            "displayAs": "222.00"
                 |          },
                 |          "2": {
                 |            "value": 333,
                 |            "displayAs": "333.00"
                 |          },
                 |          "3": {
                 |            "value": 444,
                 |            "displayAs": "444.00"
                 |          },
                 |          "4": {
                 |            "value": 555,
                 |            "displayAs": "555.00"
                 |          }
                 |        },
                 |        "connectedPartyStatus": {
                 |          "0": true,
                 |          "3": false
                 |        },
                 |        "bondsUnregulated": {
                 |          "0": true,
                 |          "1": false,
                 |          "2": true,
                 |          "3": false,
                 |          "4": true
                 |        },
                 |        "totalIncomeOrReceipts": {
                 |          "0": {
                 |            "value": 111,
                 |            "displayAs": "111.00"
                 |          },
                 |          "1": {
                 |            "value": 222,
                 |            "displayAs": "222.00"
                 |          },
                 |          "2": {
                 |            "value": 333,
                 |            "displayAs": "333.00"
                 |          },
                 |          "3": {
                 |            "value": 444,
                 |            "displayAs": "444.00"
                 |          },
                 |          "4": {
                 |            "value": 555,
                 |            "displayAs": "555.00"
                 |          }
                 |        },
                 |        "bondsCompleted": {
                 |          "0": {},
                 |          "1": {},
                 |          "2": {},
                 |          "3": {},
                 |          "4": {}
                 |        },
                 |        "bondsDisposed": {
                 |          "methodOfDisposal": {
                 |            "1": {
                 |              "0": {
                 |                "key": "Sold"
                 |              }
                 |            },
                 |            "2": {
                 |              "0": {
                 |                "key": "Transferred"
                 |              }
                 |            },
                 |            "3": {
                 |              "0": {
                 |                "key": "Other",
                 |                "value": "other disposal"
                 |              },
                 |              "1": {
                 |                "key": "Sold"
                 |              }
                 |            },
                 |            "4": {
                 |              "0": {
                 |                "key": "Transferred"
                 |              },
                 |              "1": {
                 |                "key": "Other",
                 |                "value": "other disposal"
                 |              }
                 |            }
                 |          },
                 |          "dateSold": {
                 |            "1": {
                 |              "0": "2023-02-02"
                 |            },
                 |            "3": {
                 |              "1": "2023-04-04"
                 |            }
                 |          },
                 |          "amountReceived": {
                 |            "1": {
                 |              "0": {
                 |                "value": 22,
                 |                "displayAs": "22.00"
                 |              }
                 |            },
                 |            "3": {
                 |              "1": {
                 |                "value": 44,
                 |                "displayAs": "44.00"
                 |              }
                 |            }
                 |          },
                 |          "bondsPurchaserName": {
                 |            "1": {
                 |              "0": "bonds two buyer"
                 |            },
                 |            "3": {
                 |              "1": "bonds four buyer"
                 |            }
                 |          },
                 |          "connectedPartyStatus": {
                 |            "1": {
                 |              "0": true
                 |            },
                 |            "3": {
                 |              "1": false
                 |            }
                 |          },
                 |          "totalNowHeld": {
                 |            "1": {
                 |              "0": 0
                 |            },
                 |            "2": {
                 |              "0": 33
                 |            },
                 |            "3": {
                 |              "0": 44,
                 |              "1": 0
                 |            },
                 |            "4": {
                 |              "0": 55,
                 |              "1": 5
                 |            }
                 |          },
                 |          "bondsDisposalCYAPointOfEntry": {
                 |            "1": {
                 |              "0": "NoPointOfEntry"
                 |            },
                 |            "2": {
                 |              "0": "NoPointOfEntry"
                 |            },
                 |            "3": {
                 |              "0": "NoPointOfEntry",
                 |              "1": "NoPointOfEntry"
                 |            },
                 |            "4": {
                 |              "0": "NoPointOfEntry",
                 |              "1": "NoPointOfEntry"
                 |            }
                 |          },
                 |          "bondsDisposalCompleted": {
                 |            "1": {
                 |              "0": {
                 |                "status": "JourneyCompleted"
                 |              }
                 |            },
                 |            "2": {
                 |              "0": {
                 |                "status": "JourneyCompleted"
                 |              }
                 |            },
                 |            "3": {
                 |              "0": {
                 |                "status": "JourneyCompleted"
                 |              },
                 |              "1": {
                 |                "status": "JourneyCompleted"
                 |              }
                 |            },
                 |            "4": {
                 |              "0": {
                 |                "status": "JourneyCompleted"
                 |              },
                 |              "1": {
                 |                "status": "JourneyCompleted"
                 |              }
                 |            }
                 |          }
                 |        }
                 |      },
                 |      "bondsDisposal": true
                 |    }
                 |  },
                 |  "bondsDisposalJourneyCompleted": {}
                 |}
                 |""".stripMargin)

  val cleanedSomeDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "current": "dummy-current-data",
                 |  "assets": {
                 |    "bonds": {
                 |    "unregulatedOrConnectedBondsHeld": true,
                 |    "bondsPrePopulated":{"4":false,"1":false,"0":false,"2":false,"3":false},
                 |      "bondTransactions": {
                 |        "nameOfBonds": {
                 |          "0": "first bonds - no disposals",
                 |          "2": "third bonds - partially disposed after one disposal",
                 |          "4": "fifth bonds - partially disposed after two disposals"
                 |        },
                 |        "methodOfHolding": {
                 |          "0": "01",
                 |          "2": "03",
                 |          "4": "02"
                 |        },
                 |        "dateOfAcqOrContrib": {
                 |          "0": "2001-01-01",
                 |          "4": "2005-05-05"
                 |        },
                 |        "costOfBonds": {
                 |          "0": {
                 |            "value": 111,
                 |            "displayAs": "111.00"
                 |          },
                 |          "2": {
                 |            "value": 333,
                 |            "displayAs": "333.00"
                 |          },
                 |          "4": {
                 |            "value": 555,
                 |            "displayAs": "555.00"
                 |          }
                 |        },
                 |        "connectedPartyStatus": {
                 |          "0": true
                 |        },
                 |        "bondsUnregulated": {
                 |          "0": true,
                 |          "2": true,
                 |          "4": true
                 |        },
                 |        "bondsCompleted": {
                 |          "0": {},
                 |          "2": {},
                 |          "4": {}
                 |        }
                 |      }
                 |    }
                 |  }
                 |}
                 |""".stripMargin)

  val noDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "assets": {
                 |    "bonds": {
                 |      "unregulatedOrConnectedBondsHeld": true,
                 |      "recordVersion": "001",
                 |      "bondTransactions": {
                 |        "nameOfBonds": {
                 |          "0": "first bonds - no disposals",
                 |          "1": "second bonds - no disposals",
                 |          "2": "third bonds - no disposals",
                 |          "3": "fourth bonds - no disposals",
                 |          "4": "fifth bonds - no disposals"
                 |        },
                 |        "methodOfHolding": {
                 |          "0": "01",
                 |          "1": "02",
                 |          "2": "03",
                 |          "3": "01",
                 |          "4": "02"
                 |        },
                 |        "dateOfAcqOrContrib": {
                 |          "0": "2001-01-01",
                 |          "1": "2002-02-02",
                 |          "3": "2004-04-04",
                 |          "4": "2005-05-05"
                 |        },
                 |        "costOfBonds": {
                 |          "0": {
                 |            "value": 111,
                 |            "displayAs": "111.00"
                 |          },
                 |          "1": {
                 |            "value": 222,
                 |            "displayAs": "222.00"
                 |          },
                 |          "2": {
                 |            "value": 333,
                 |            "displayAs": "333.00"
                 |          },
                 |          "3": {
                 |            "value": 444,
                 |            "displayAs": "444.00"
                 |          },
                 |          "4": {
                 |            "value": 555,
                 |            "displayAs": "555.00"
                 |          }
                 |        },
                 |        "connectedPartyStatus": {
                 |          "0": true,
                 |          "3": false
                 |        },
                 |        "bondsUnregulated": {
                 |          "0": true,
                 |          "1": false,
                 |          "2": true,
                 |          "3": false,
                 |          "4": true
                 |        },
                 |        "totalIncomeOrReceipts": {
                 |          "0": {
                 |            "value": 111,
                 |            "displayAs": "111.00"
                 |          },
                 |          "1": {
                 |            "value": 222,
                 |            "displayAs": "222.00"
                 |          },
                 |          "2": {
                 |            "value": 333,
                 |            "displayAs": "333.00"
                 |          },
                 |          "3": {
                 |            "value": 444,
                 |            "displayAs": "444.00"
                 |          },
                 |          "4": {
                 |            "value": 555,
                 |            "displayAs": "555.00"
                 |          }
                 |        },
                 |        "bondsCompleted": {
                 |          "0": {},
                 |          "1": {},
                 |          "2": {},
                 |          "3": {},
                 |          "4": {}
                 |        }
                 |      }
                 |    }
                 |  }
                 |}
                 |""".stripMargin)

  val cleanedNoDisposalsData: JsValue =
    Json.parse("""
                 |{
                 |  "current": "dummy-current-data",
                 |  "assets": {
                 |    "bonds": {
                 |    "unregulatedOrConnectedBondsHeld": true,
                 |    "bondsPrePopulated":{"4":false,"1":false,"0":false,"2":false,"3":false},
                 |      "bondTransactions": {
                 |        "nameOfBonds": {
                 |          "0": "first bonds - no disposals",
                 |          "1": "second bonds - no disposals",
                 |          "2": "third bonds - no disposals",
                 |          "3": "fourth bonds - no disposals",
                 |          "4": "fifth bonds - no disposals"
                 |        },
                 |        "methodOfHolding": {
                 |          "0": "01",
                 |          "1": "02",
                 |          "2": "03",
                 |          "3": "01",
                 |          "4": "02"
                 |        },
                 |        "dateOfAcqOrContrib": {
                 |          "0": "2001-01-01",
                 |          "1": "2002-02-02",
                 |          "3": "2004-04-04",
                 |          "4": "2005-05-05"
                 |        },
                 |        "costOfBonds": {
                 |          "0": {
                 |            "value": 111,
                 |            "displayAs": "111.00"
                 |          },
                 |          "1": {
                 |            "value": 222,
                 |            "displayAs": "222.00"
                 |          },
                 |          "2": {
                 |            "value": 333,
                 |            "displayAs": "333.00"
                 |          },
                 |          "3": {
                 |            "value": 444,
                 |            "displayAs": "444.00"
                 |          },
                 |          "4": {
                 |            "value": 555,
                 |            "displayAs": "555.00"
                 |          }
                 |        },
                 |        "connectedPartyStatus": {
                 |          "0": true,
                 |          "3": false
                 |        },
                 |        "bondsUnregulated": {
                 |          "0": true,
                 |          "1": false,
                 |          "2": true,
                 |          "3": false,
                 |          "4": true
                 |        },
                 |        "bondsCompleted": {
                 |          "0": {},
                 |          "1": {},
                 |          "2": {},
                 |          "3": {},
                 |          "4": {}
                 |        }
                 |      }
                 |    }
                 |  }
                 |}
                 |""".stripMargin)
}
