package controllers

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import pages.$className$Page
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.MoneyFormProvider
import views.html.MultipleQuestionView
import $className;format="cap"$Controller._
$if(!index.empty)$
import config.RefinedTypes.$index$
import eu.timepit.refined.refineMV
$endif$

import scala.concurrent.Future

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  $if(index.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)
  $else$
  private val index = refineMV[$index$.Refined](1)
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, NormalMode)
  $endif$

  "$className;format="cap"$Controller" - {

    $if(index.empty)$
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(viewModel(srn, form(injected[MultipleQuestionFormProvider]), NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), money) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(viewModel(srn, form(injected[MultipleQuestionFormProvider]).fill(money), NormalMode))
    })
    $else$
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(viewModel(srn, index, form(injected[MultipleQuestionFormProvider]), NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn, index), money) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(viewModel(srn, index, form(injected[MultipleQuestionFormProvider]).fill(money), NormalMode))
    })
    $endif$

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
