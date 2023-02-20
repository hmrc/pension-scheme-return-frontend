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

package controllers

import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.SchemeBankAccountPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import viewmodels.DisplayMessage.SimpleMessage
import views.html.CheckYourAnswersView


class SchemeBankAccountCheckYourAnswersControllerSpec extends ControllerBaseSpec with ControllerBehaviours with ScalaCheckPropertyChecks {

  def onwardRoute = Call("GET", "/foo")

  val bankAccount = bankAccountGen.sample.value
  override val userAnswers = emptyUserAnswers.set(SchemeBankAccountPage(srn), bankAccount).get

  lazy val onPageLoad = routes.SchemeBankAccountCheckYourAnswersController.onPageLoad(srn)
  lazy val onSubmit = routes.SchemeBankAccountCheckYourAnswersController.onSubmit(srn)

  "SchemeBankAccountCheckYourAnswersController" should {

    lazy val viewModel = SchemeBankAccountCheckYourAnswersController.viewModel(srn, bankAccount)

    behave like renderPrePopView(onPageLoad, SchemeBankAccountPage(srn), bankAccount) { implicit app => implicit request =>
      val view = injected[CheckYourAnswersView]
      view(viewModel)
    }

    behave like redirectWhenCacheEmpty(onPageLoad, routes.SchemeBankAccountController.onPageLoad(srn, NormalMode))

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)
    behave like journeyRecoveryPage("onSubmit", onSubmit)

    behave like redirectNextPage(onSubmit)
  }

  "SchemeBankAccountCheckYourAnswers.viewModel" should {

    val viewModel = SchemeBankAccountCheckYourAnswersController.viewModel _

    "have the correct message key for title" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).title.key mustBe "checkYourAnswers.title"
      }
    }

    "have the correct message key for heading" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).heading.key mustBe "checkYourAnswers.heading"
      }
    }

    "have the correct message key for bank name" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.key.key) must contain("schemeBankDetails.bankName.heading")
      }
    }

    "have the correct message key for account number" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.key.key) must contain("schemeBankDetails.accountNumber.heading")
      }
    }

    "have the correct message key for sort code" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.key.key) must contain("schemeBankDetails.sortCode.heading")
      }
    }

    "have the correct bank name" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.value.key) must contain(bankAccount.bankName)
      }
    }

    "have the correct account number" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.value.key) must contain(bankAccount.accountNumber)
      }
    }

    "have the correct sort code" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.value.key) must contain(bankAccount.sortCode)
      }
    }

    "have the correct actions" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        val content = SimpleMessage("site.change")
        val href = routes.SchemeBankAccountController.onPageLoad(srn, NormalMode).url

        viewModel(srn, bankAccount).rows.flatMap(_.actions) must contain allElementsOf List.fill(3)(content -> href)
      }
    }

    "have the correct on submit value" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).onSubmit mustBe routes.SchemeBankAccountCheckYourAnswersController.onSubmit(srn)
      }
    }
  }
}

