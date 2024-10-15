$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.YesNoPageFormProvider
import views.html.YesNoPageView
import controllers.ControllerBaseSpec
import $className;format="cap"$Controller._
import scala.concurrent.Future

$if(directory.empty)$
import pages.nonsipp.$className;format="cap"$Page
$else$
import pages.nonsipp.$directory$.$className$Page
$endif$

$if(!index.empty)$
import config.RefinedTypes._
import eu.timepit.refined.refineMV
$endif$

$if(!requiredPage.empty)$
  $if(directory.empty)$
  import pages.nonsipp.$requiredPage$
  $else$
  import pages.nonsipp.$directory$.$requiredPage$
  $endif$
$endif$

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  $! Generic !$
  $if(index.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)
  $else$
  private val index = refineMV[$index$.Refined](1)
  $if(secondaryIndex.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, NormalMode)
  $else$
  private val secondaryIndex = refineMV[$secondaryIndex$.Refined](1)
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, secondaryIndex, NormalMode)
  $endif$
  $endif$

  $if(!requiredPage.empty)$
  private val userAnswers = defaultUserAnswers.unsafeSet($requiredPage$(srn, index), ???)
  $endif$

  "$className;format="cap"$Controller" - {

    $! Generic (change view and form value) !$
    act like renderView(onPageLoad$if(!requiredPage.empty)$, userAnswers$endif$) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, $if(!requiredPage.empty)$???, $endif$$if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$NormalMode))
    }

    act like renderPrePopView(onPageLoad, $className;format="cap"$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), true$if(!requiredPage.empty)$, userAnswers$endif$) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, $if(!requiredPage.empty)$???, $endif$$if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$NormalMode))
    }
    $! Generic end !$

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
