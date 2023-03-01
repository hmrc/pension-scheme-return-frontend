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

  private val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode).url
  private val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode).url

  private val redirectUrlYes = routes.UnauthorisedController.onPageLoad.url
  private val redirectUrlNo = routes.UnauthorisedController.onPageLoad.url

  "$className;format="cap"$Controller" should {

    behave like renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel())
    }

    behave like redirectNextPage(onSubmit, redirectUrlYes, "value" -> "true")
    behave like redirectNextPage(onSubmit, redirectUrlNo, "value" -> "false")
  }
}
