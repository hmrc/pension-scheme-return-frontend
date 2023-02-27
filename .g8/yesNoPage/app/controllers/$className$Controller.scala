package controllers

import controllers.actions._
import forms.YesNoPageFormProvider
import javax.inject.Inject
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.$className$Page
import play.api.data.Form
import viewmodels.models.YesNoPageViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import $className;format="cap"$Controller._

import scala.concurrent.{ExecutionContext, Future}

class $className;format="cap"$Controller @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         allowAccess: AllowAccessActionProvider,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: YesNoPageFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: YesNoPageView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = $className;format="cap"$Controller.form(formProvider)

  def onPageLoad(srn:Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get($className$Page(srn)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn), value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn), mode, updatedAnswers))
      )
  }
}

object $className;format="cap"$Controller {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "$className;format="decap"$.error.required"
  )

  def viewModel(srn: Srn, mode: Mode): YesNoPageViewModel = YesNoPageViewModel(
    "$className;format="decap"$.title",
    "$className;format="decap"$.heading",
    controllers.routes.$className;format="cap"$Controller.onSubmit(srn, mode)
  )
}