package controllers

import controllers.actions._
import forms.TextFormProvider
import javax.inject.Inject
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.models.TextAreaViewModel
import viewmodels.implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TextAreaView
import services.SaveService
import pages.$className$Page
import $className;format="cap"$Controller._

import scala.concurrent.{ExecutionContext, Future}

class $className;format="cap"$Controller @Inject()(
                                         override val messagesApi: MessagesApi,
                                         saveService: SaveService,
                                         navigator: Navigator,
                                         identifyAndRequireData: IdentifyAndRequireData,
                                         formProvider: TextFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: TextAreaView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = $className;format="cap"$Controller.form(formProvider)

  def onPageLoad(srn:Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.get($className$Page(srn)).fold(form)(form.fill)
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
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "$className;format="decap"$.error.required",
    "$className;format="decap"$.error.length",
    "$className;format="decap"$.error.invalid",
  )

  def viewModel(srn: Srn, mode: Mode): TextAreaViewModel = TextAreaViewModel(
    "$className;format="decap"$.title",
    "$className;format="decap"$.heading",
    controllers.routes.$className;format="cap"$Controller.onSubmit(srn, mode)
  )
}