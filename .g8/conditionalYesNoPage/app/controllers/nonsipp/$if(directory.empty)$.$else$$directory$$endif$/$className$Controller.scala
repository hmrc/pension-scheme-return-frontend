$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

import controllers.actions._
import forms.YesNoPageFormProvider
import models._
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import controllers.PSRController
import viewmodels.models._
import play.api.i18n._
import play.api.mvc._
import views.html.ConditionalYesNoPageView
import services.SaveService
import viewmodels.implicits._
import config.Constants._
$if(directory.empty)$
import pages.$className$Page
$else$
import pages.nonsipp.$directory$.$className$Page
$endif$
import $className;format="cap"$Controller._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import forms.mappings.Mappings
import forms.mappings.errors._
$if(!index.empty)$
import config.RefinedTypes.$index$
$endif$

class $className;format="cap"$Controller @Inject()(
   override val messagesApi: MessagesApi,
   saveService: SaveService,
   @Named("non-sipp") navigator: Navigator,
   identifyAndRequireData: IdentifyAndRequireData,
   formProvider: YesNoPageFormProvider,
   val controllerComponents: MessagesControllerComponents,
   view: ConditionalYesNoPageView
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn), ConditionalYesNo(value)))
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn, index), ConditionalYesNo(value)))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn, index), mode, updatedAnswers))
      )
  }
  $endif$
}

object $className;format="cap"$Controller {

  private val noFormErrors = InputFormErrors.textArea(
    "$className;format="decap"$.no.conditional.error.required",
    "$className;format="decap"$.no.conditional.error.invalid",
    "$className;format="decap"$.no.conditional.error.length"
  )

  private val yesFormErrors = InputFormErrors(
    "$className;format="decap"$.yes.conditional.error.required",
    "$className;format="decap"$.yes.conditional.error.invalid",
    (maxTextArea, "$className;format="decap"$.yes.conditional.error.length")
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, String]] = formProvider.conditional(
    "$className;format="decap"$.error.required",
    mappingNo = Mappings.textArea(noFormErrors),
    mappingYes = Mappings.input(yesFormErrors)
  )

  $if(index.empty)$
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[ConditionalYesNoPageViewModel] = FormPageViewModel[ConditionalYesNoPageViewModel](
  $else$
  def viewModel(srn: Srn, index: $index$, mode: Mode): FormPageViewModel[ConditionalYesNoPageViewModel] = FormPageViewModel[ConditionalYesNoPageViewModel](
  $endif$
    title = "$className;format="decap"$.title",
    heading = "$className;format="decap"$.heading",
    ConditionalYesNoPageViewModel(
      $if(!hint.empty)$
      hint = Some("$className;format="decap"$.hint"),
      $endif$
      yes = YesNoViewModel
        .Conditional("$className;format="decap"$.yes.conditional", FieldType.Input),
      no = YesNoViewModel
        .Conditional("$className;format="decap"$.no.conditional", FieldType.Textarea)
    ),
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