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


class SchemeBankAccountCheckYourAnswersControllerSpec extends ControllerBaseSpec with ScalaCheckPropertyChecks {

  def onwardRoute = Call("GET", "/foo")

  val bankAccount = bankAccountGen.sample.value
  override val userAnswers = emptyUserAnswers.set(SchemeBankAccountPage(srn), bankAccount).get

  lazy val onPageLoad = routes.SchemeBankAccountCheckYourAnswersController.onPageLoad(srn).url
  lazy val onSubmit = routes.SchemeBankAccountCheckYourAnswersController.onSubmit(srn).url


  "SchemeBankAccountCheckYourAnswersController.onPageLoad" should {

    "return OK and the correct view for a GET" in {

      running(_ => applicationBuilder(userAnswers = Some(userAnswers))) { implicit app =>

        val view = injected[CheckYourAnswersView]
        val request = FakeRequest(GET, onPageLoad)

        val result = route(app, request).value
        val expectedView = view(SchemeBankAccountCheckYourAnswersController.viewModel(srn, bankAccount))(request, createMessages(app))

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
      }
    }

    "must redirect to scheme bank details page when bank account not in cache" in {

      running(_ => applicationBuilder(userAnswers = Some(emptyUserAnswers))) { implicit app =>

        val view = injected[CheckYourAnswersView]
        val request = FakeRequest(GET, onPageLoad)

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.SchemeBankAccountController.onPageLoad(srn, NormalMode).url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {

        val request = FakeRequest(GET, onPageLoad)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "SchemeBankAccountCheckYourAnswers.onSubmit" should {

    "redirect to the next page" in {

      val fakeNavigatorApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
        )

      running(_ => fakeNavigatorApplication) { app =>

        val request = FakeRequest(GET, onSubmit)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {

        val request = FakeRequest(GET, onSubmit)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "SchemeBankAccountCheckYourAnswers.viewModel" should {

    val viewModel = SchemeBankAccountCheckYourAnswersController.viewModel _

    "have the correct message key for title" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).title.key mustBe "schemeBankAccountCheckYourAnswers.title"
      }
    }

    "have the correct message key for heading" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).heading.key mustBe "schemeBankAccountCheckYourAnswers.heading"
      }
    }

    "have the correct message key for bank name" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.key.key) must contain("schemeBankAccountCheckYourAnswers.bankName")
      }
    }

    "have the correct message key for account number" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.key.key) must contain("schemeBankAccountCheckYourAnswers.accountNumber")
      }
    }

    "have the correct message key for sort code" in {

      forAll(srnGen, bankAccountGen) { (srn, bankAccount) =>

        viewModel(srn, bankAccount).rows.map(_.key.key) must contain("schemeBankAccountCheckYourAnswers.sortCode")
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

