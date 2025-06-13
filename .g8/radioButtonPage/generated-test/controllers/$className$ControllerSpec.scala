package controllers

import controllers.ControllerBaseSpec
import forms.$className$FormProvider
import models.{NormalMode, $className$, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.$className$Page
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.$className$View
import forms.RadioListFormProvider
import $className$Controller._
import views.html.RadioListView

import scala.concurrent.Future

class $className$ControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  lazy val onPageLoad = routes.$className$Controller.onPageLoad(srn, NormalMode)
  lazy val onSubmit = routes.$className$Controller.onSubmit(srn, NormalMode)

  "$className$ Controller" - {

    act.like(renderView(onPageLoad) { implicit app =>
      implicit request =>
        injected[RadioListView].apply(form(injected[RadioListFormProvider]), viewModel(srn, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className$Page(srn), ???) {
      implicit app =>
        implicit request =>
          injected[RadioListView]
            .apply(form(injected[RadioListFormProvider]).fill(???), viewModel(srn, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> ???))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
