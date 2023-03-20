package controllers

import $className;format="cap"$Controller._
import controllers.actions._
import forms.TripleIntFormProvider
import forms.mappings.errors.IntFormErrors
import javax.inject.Inject
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.$className$Page
import play.api.data.Form
import viewmodels.models.TripleIntViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.models.TripleIntViewModel
import views.html.TripleIntView
import services.SaveService

import scala.concurrent.{ExecutionContext, Future}

class $className;format="cap"$Controller @Inject()(
                                         override val messagesApi: MessagesApi,
                                         saveService: SaveService,
                                         navigator: Navigator,
                                         identifyAndRequireData: IdentifyAndRequireData,
                                         formProvider: TripleIntFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: TripleIntView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = $className;format="cap"$Controller.form(formProvider)

  def onPageLoad(srn:Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = form.fromUserAnswers($className$Page(srn))
      Ok(view(preparedForm, viewModel(srn, mode)))
  }

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
}

object $className;format="cap"$Controller {

  private val field1Errors: IntFormErrors =
    IntFormErrors(
      "$className;format="decap"$.field1.error.required",
      "$className;format="decap"$.field1.error.wholeNumber",
      "$className;format="decap"$.field1.error.nonNumber",
      ($field2MaxValue$, "$className;format="decap"$.field1.error.max")
    )

  private val field2Errors: IntFormErrors =
    IntFormErrors(
      "$className;format="decap"$.field2.error.required",
      "$className;format="decap"$.field2.error.wholeNumber",
      "$className;format="decap"$.field2.error.nonNumber",
      ($field1MaxValue$, "$className;format="decap"$.field2.error.max")
    )

  private val field3Errors: IntFormErrors =
    IntFormErrors(
      "$className;format="decap"$.field3.error.required",
      "$className;format="decap"$.field3.error.wholeNumber",
      "$className;format="decap"$.field3.error.nonNumber",
      ($field3MaxValue$, "$className;format="decap"$.field3.error.max")
    )

  def form(formProvider: TripleIntFormProvider): Form[(Int, Int, Int)] = formProvider(
    field1Errors,
    field2Errors,
    field3Errors
  )

  def viewModel(srn: Srn, mode: Mode): TripleIntViewModel = TripleIntViewModel(
    "$className;format="decap"$.title",
    "$className;format="decap"$.heading",
    "$className;format="decap"$.field1",
    "$className;format="decap"$.field2",
    "$className;format="decap"$.field3",
    controllers.routes.$className;format="cap"$Controller.onSubmit(srn, mode)
  )
}