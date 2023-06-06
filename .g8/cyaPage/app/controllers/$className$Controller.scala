package controllers

import controllers.actions._
import play.api.i18n._
import play.api.mvc._
import navigation.Navigator
import models.Mode
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersViewModel, FormPageViewModel, SummaryAction}
import viewmodels.implicits._
import viewmodels.DisplayMessage._
import views.html.CheckYourAnswersView
import $className$Controller._
import pages.$className;format="cap"$Page
import models.SchemeId.Srn

import javax.inject.{Inject, Named}

class $className;format="cap"$Controller @Inject()(
   override val messagesApi: MessagesApi,
   @Named("non-sipp") navigator: Navigator,
   identifyAndRequireData: IdentifyAndRequireData,
   val controllerComponents: MessagesControllerComponents,
   view: CheckYourAnswersView
) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
      Ok(view(viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
      Redirect(navigator.nextPage($className;format="cap"$Page(srn), mode, request.userAnswers))
  }
}

object $className;format="cap"$Controller {
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[CheckYourAnswersViewModel] = FormPageViewModel[CheckYourAnswersViewModel](
    title = "checkYourAnswers.title",
    heading = "checkYourAnswers.heading",
    description = Some(ParagraphMessage("$className;format="decap"$.paragraph")),
    page = CheckYourAnswersViewModel(rows(srn)),
    refresh = None,
    buttonText = "site.continue",
    onSubmit = routes.$className$Controller.onSubmit(srn, mode)
  )

  private def rows(srn: Srn): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("row 1 key", "row 1 value")
      .withAction(
        SummaryAction("site.change", controllers.routes.UnauthorisedController.onPageLoad().url)
          .withVisuallyHiddenContent("row 1 key")
      ),
    CheckYourAnswersRowViewModel("row 2 key", "row 2 value")
      .withAction(
        SummaryAction("site.change", controllers.routes.UnauthorisedController.onPageLoad().url)
          .withVisuallyHiddenContent("row 2 key")
      )
  )
}
