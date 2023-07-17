package controllers

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.YesNoPageFormProvider
import views.html.YesNoPageView
import controllers.nonsipp.loansmadeoroutstanding.OutstandingArrearsOnLoanController._
import pages.nonsipp.loansmadeoroutstanding.OutstandingArrearsOnLoanPage

import scala.concurrent.Future

class OutstandingArrearsOnLoanControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.OutstandingArrearsOnLoanController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.OutstandingArrearsOnLoanController.onSubmit(srn, NormalMode)

  "OutstandingArrearsOnLoanController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, OutstandingArrearsOnLoanPage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
