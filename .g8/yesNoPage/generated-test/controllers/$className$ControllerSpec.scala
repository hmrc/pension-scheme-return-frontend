package controllers

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import pages.$className$Page
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.YesNoPageFormProvider
import views.html.YesNoPageView
import $className;format="cap"$Controller._

import scala.concurrent.Future

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)

  "$className;format="cap"$Controller" should {

    act.like renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, NormalMode))
    }

    act.like renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, NormalMode))
    }

    act.like redirectNextPage(onSubmit, "value" -> "true")
    act.like redirectNextPage(onSubmit, "value" -> "false")

    act.like journeyRecoveryPage("onPageLoad", onPageLoad)

    act.like saveAndContinue(onSubmit, "value" -> "true")

    act.like invalidForm(onSubmit)
  }
}
