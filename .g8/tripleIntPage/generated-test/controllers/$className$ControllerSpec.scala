package controllers

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import pages.$className$Page
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.TripleIntView
import $className;format="cap"$Controller._

import scala.concurrent.Future

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)

  "$className;format="cap"$Controller" should {

    act.like renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[TripleIntView]

      view(form(injected[TripleIntFormProvider]), viewModel(srn, NormalMode))
    }

    act.like renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), (1, 2, 3)) { implicit app => implicit request =>
      val view = injected[TripleIntView]

      view(form(injected[TripleIntFormProvider]).fill((1, 2, 3)), viewModel(srn, NormalMode))
    }

    act.like journeyRecoveryPage("onPageLoad", onPageLoad)

    act.like saveAndContinue(onSubmit, "value.1" -> "1", "value.2" -> "2", "value.3" -> "3")

    act.like invalidForm(onSubmit)
  }
}
