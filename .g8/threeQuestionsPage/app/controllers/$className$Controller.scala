package controllers

import controllers.actions._
import forms.MultipleQuestionFormProvider
import javax.inject.{Inject, Named}
import models._
import navigation.Navigator
import pages.$className$Page
import services.SaveService
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.data.Form
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.MultipleQuestionView
import viewmodels.models.MultipleQuestionsViewModel._
import viewmodels.models._
import viewmodels.implicits._
import $className;format="cap"$Controller._
import forms.mappings.errors._
import pages.$className;format="cap"$Page
import models.SchemeId.Srn
import forms.mappings.Mappings
$if(!index.empty)$
import config.Refined.$index$
$endif$

import scala.concurrent.{ExecutionContext, Future}

class $className;format="cap"$Controller @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  saveService: SaveService
)(implicit ec: ExecutionContext) extends PSRController {

  $if(index.empty)$
  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn), form)
      Ok(view(viewModel(srn, preparedForm, mode)))
  }
  $else$
  def onPageLoad(srn: Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn, index), form)
      Ok(view(viewModel(srn, index, preparedForm, mode)))
  }
  $endif$

  $if(index.empty)$
  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(viewModel(srn, formWithErrors, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn), value))
            _              <- saveService.set(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn), mode, updatedAnswers))
      )
  }
  $else$
  def onSubmit(srn: Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(viewModel(srn, index, formWithErrors, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn, index), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn, index), mode, updatedAnswers))
      )
  }
  $endif$
}

object $className;format="cap"$Controller {
  private val field1Errors: $field1Type$FormErrors =
    $field1Type$FormErrors(
      "$className;format="decap"$.field1.error.required"
    )

  private val field2Errors: $field2Type$FormErrors =
    $field2Type$FormErrors(
      "$className;format="decap"$.field2.error.required",
      (123, "$className;format="decap"$.field2.error.required")
    )

  private val field3Errors: $field3Type$FormErrors =
    $field3Type$FormErrors(
      "$className;format="decap"$.field3.error.required",
      (123, "$className;format="decap"$.field3.error.required")
    )

  val form: Form[($field1Type$, $field2Type$, $field3Type$)] =
    MultipleQuestionFormProvider(
      Mappings.$field1Type;format="decap"$(field1Errors),
      Mappings.$field2Type;format="decap"$(field2Errors),
      Mappings.$field3Type;format="decap"$(field3Errors)
    )

  def viewModel(
     srn: Srn,
     $if(!index.empty)$index: $index$,$endif$
     form: Form[($field1Type$, $field2Type$, $field3Type$)],
     mode: Mode,
   ): FormPageViewModel[TripleQuestion[$field1Type$, $field2Type$, $field3Type$]] = FormPageViewModel(
    title = "$className;format="decap"$.title",
    heading = "$className;format="decap"$.heading",
    $if(paragraph.empty)$
    description = None,
    $else$
    description = Some("$className;format="decap"$.paragraph"),
    $endif$
    page = TripleQuestion(
      form,
      QuestionField.$field1Type;format="decap"$("$className;format="decap"$.field1.label"),
      QuestionField.$field2Type;format="decap"$("$className;format="decap"$.field2.label"),
      QuestionField.$field3Type;format="decap"$("$className;format="decap"$.field3.label"),
    ),
    refresh = None,
    buttonText = "site.saveAndContinue",
    details = None,
    $if(index.empty)$
    onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, mode)
    $else$
    onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, mode)
    $endif$
  )
}
