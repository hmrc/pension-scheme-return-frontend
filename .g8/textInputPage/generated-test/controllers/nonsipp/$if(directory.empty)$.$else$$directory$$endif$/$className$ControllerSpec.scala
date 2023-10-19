$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import pages.$className$Page
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.TextFormProvider
import views.html.TextInputView
import $className;format="cap"$Controller._
$if(!index.empty)$
import config.Refined.$index$
import eu.timepit.refined.refineMV
$endif$

import scala.concurrent.Future

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  $if(index.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)
  $else$
  private val index = refineMV[$index$.Refined](1)
  private lazy val onPageLoad = routes.$className; format = "cap" $Controller.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.$className; format = "cap" $Controller.onSubmit(srn, index, NormalMode)
  $endif$

  "$className;format="cap"$Controller" - {

    $if(index.empty)$
      act.like(renderView(onPageLoad) { implicit app => implicit request =>
        injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, NormalMode))
      })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), "test") { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]).fill("test"), viewModel(srn, NormalMode))
    })
    $else$
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn, index), "test") { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]).fill("test"), viewModel(srn, index, NormalMode))
    })
    $endif$

    act.like(redirectNextPage(onSubmit, "value" -> "test"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "test"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
