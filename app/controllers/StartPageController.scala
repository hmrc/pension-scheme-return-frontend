package controllers

import controllers.actions._
import models.SchemeId.Srn
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.StartPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.ContentPageViewModel
import views.html.ContentPageView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartPageController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       allowAccess: AllowAccessActionProvider,
                                       getData: DataRetrievalAction,
                                       createData: DataCreationAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ContentPageView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData) {
    implicit request =>
      Ok(view(StartPageController.viewModel(mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData) {
    implicit request =>
      Redirect(navigator.nextPage(StartPage(srn), mode, request.userAnswers.getOrElse(UserAnswers(request.getUserId))))
  }
}

object StartPageController {

  def viewModel(mode: Mode): ContentPageViewModel = ContentPageViewModel(

  )

}