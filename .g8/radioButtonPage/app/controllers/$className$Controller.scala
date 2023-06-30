package controllers

import controllers.actions._
import forms.RadioListFormProvider
import javax.inject.{Inject, Named}
import models.Mode
import navigation.Navigator
import pages.$className;format="cap"$Page
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RadioListView
import $className;format="cap"$Controller._
import models.SchemeId.Srn
import utils.FormUtils.FormOps
import viewmodels.implicits._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

class $className;format="cap"$Controller @Inject()(
                                       override val messagesApi: MessagesApi,
                                       saveService: SaveService,
                                       @Named("non-sipp") navigator: Navigator,
                                       identifyAndRequireData: IdentifyAndRequireData,
                                       formProvider: RadioListFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: RadioListView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = $className;format="cap"$Controller.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Ok(
      view(
        form.fromUserAnswers($className;format="cap"$Page(srn)),
        $className;format="cap"$Controller.viewModel(srn, mode)
      )
    )
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        errors =>
          Future.successful(
            BadRequest(view(errors, $className;format="cap"$Controller.viewModel(srn, mode)))
          ),
        success =>
          for {
            userAnswers <- Future.fromTry(request.userAnswers.set($className;format="cap"$Page(srn), success))
            _ <- saveService.save(userAnswers)
          } yield {
            Redirect(navigator.nextPage($className;format="cap"$Page(srn), mode, userAnswers))
          }
      )
  }
}

object $className;format="cap"$Controller {

  def form(formProvider: RadioListFormProvider) =
    formProvider("$className;format="decap"$.error.required")

  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[RadioListViewModel] = RadioListViewModel(
    "$className;format="decap"$.title",
    "$className;format="decap"$.heading",
    List(
      RadioListRowViewModel("$className;format="decap"$.option1", "test"),
      RadioListRowViewModel("$className;format="decap"$.option2", "test")
    ),
    routes.$className;format="cap"$Controller.onSubmit(srn, mode)
  )
}
