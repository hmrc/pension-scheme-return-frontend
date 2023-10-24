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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import services.SaveService
import $className;format="cap"$Controller._
import viewmodels.implicits._
$if(directory.empty)$
import pages.nonsipp.$className$Page
$else$
import pages.nonsipp.$directory$.$className$Page
$endif$

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
$if(!index.empty)$
import config.Refined._
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

  $if(index.empty)$
  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn), form)
      Ok(view(preparedForm, viewModel(srn, mode)))
  }
  $else$
  def onPageLoad(srn: Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }
  $endif$

  $if(index.empty)$
  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn), mode, updatedAnswers))
      )
  }
  $else$
  def onSubmit(srn: Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn, index), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn, index), mode, updatedAnswers))
      )
  }
  $endif$
}

object $className;format="cap"$Controller {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "$className;format="decap"$.error.required"
  )

  $if(index.empty)$
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
  $else$
  def viewModel(srn: Srn, index: $index$, mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
  $endif$
    "$className;format="decap"$.title",
    "$className;format="decap"$.heading",
    $if(index.empty)$
      $if(directory.empty)$
      controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, mode)
      $else$
      controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, mode)
      $endif$
    $else$
    $if(directory.empty)$
      controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, index, mode)
      $else$
      controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, index, mode)
      $endif$
    $endif$
  )
}