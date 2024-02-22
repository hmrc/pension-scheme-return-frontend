$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import controllers.PSRController
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.YesNoPageView
import services.SaveService
import $className;format="cap"$Controller._
import viewmodels.implicits._
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

$if(directory.empty)$
import pages.nonsipp.$className;format="cap"$Page
$else$
import pages.nonsipp.$directory$.$className$Page
$endif$

$if(!index.empty)$
import config.Refined._
$endif$

$if(!requiredPage.empty)$
  $if(directory.empty)$
  import pages.nonsipp.$requiredPage$
  $else$
  import pages.nonsipp.$directory$.$requiredPage$
  $endif$
$endif$

class $className;format="cap"$Controller @Inject()(
   override val messagesApi: MessagesApi,
   saveService: SaveService,
   @Named("non-sipp") navigator: Navigator,
   identifyAndRequireData: IdentifyAndRequireData,
   formProvider: YesNoPageFormProvider,
   val controllerComponents: MessagesControllerComponents,
   view: YesNoPageView
)(implicit ec: ExecutionContext) extends PSRController {

  private val form = $className;format="cap"$Controller.form(formProvider)

  $! Generic functions (viewmodel might accept extra params) !$
  def onPageLoad(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.get($className$Page(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$)).fold(form)(form.fill)
      $if(!requiredPage.empty)$
      request.userAnswers.get($requiredPage$(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$)).getOrRecoverJourney { requiredPage =>
      $endif$
        Ok(view(preparedForm, viewModel(srn, $if(!requiredPage.empty) $requiredPage, $endif$$if(!index.empty) $index, $endif$$if(!secondaryIndex.empty) $secondaryIndex, $endif$mode)))
      $if(!requiredPage.empty)$
      }
      $endif$
  }

  def onSubmit(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          $if(!requiredPage.empty) $
            request.userAnswers.get($requiredPage$(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$)).getOrRecoverJourney { requiredPage =>
          $endif$
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, $if(!requiredPage.empty) $requiredPage, $endif$$if(!index.empty) $index, $endif$$if(!secondaryIndex.empty) $secondaryIndex, $endif$mode))))
          $if(!requiredPage.empty) $
            }
          $endif$
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$), mode, updatedAnswers))
      )
  }
  $! Generic functions end !$
}

object $className;format="cap"$Controller {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "$className;format="decap"$.error.required"
  )

  def viewModel(srn: Srn, $if(!requiredPage.empty)$requiredPage: ???, $endif$$if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "$className;format="decap"$.title",
    "$className;format="decap"$.heading",
    $! Generic onSubmit !$
    $if(directory.empty)$
    controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
    $else$
    controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
    $endif$
    $! Generic onSubmit end !$
  )
}