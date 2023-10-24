package controllers.nonsipp.memberpayments

import config.Refined.{Max50, Max5000}
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.memberpayments.routes
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.{ConditionalYesNo, Crn, Mode}
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class EmployerCompanyCrnController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    saveService: SaveService,
                                    @Named("non-sipp") navigator: Navigator,
                                    identifyAndRequireData: IdentifyAndRequireData,
                                    formProvider: YesNoPageFormProvider,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: ConditionalYesNoPageView
                                  )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {
  val form: Form[Either[String, Crn]] = EmployerCompanyCrnController.form(formProvider)
  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(EmployerCompanyCrnController(srn, landOrPropertyIndex)).sync { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(EmployerCompanyCrnPage(srn, landOrPropertyIndex), form)
        Ok(
          view(
            preparedForm,
            EmployerCompanyCrnController.viewModel(srn, landOrPropertyIndex, mode, companyName)
          )
        )
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(CompanyBuyerNamePage(srn, landOrPropertyIndex)).async { companyName =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      EmployerCompanyCrnController
                        .viewModel(srn, landOrPropertyIndex,  mode, companyName)
                    )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(EmployerCompanyCrnPage(srn, landOrPropertyIndex), ConditionalYesNo(value))
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(EmployerCompanyCrnPage(srn, landOrPropertyIndex), mode, updatedAnswers)
            )
        )
    }
}

object EmployerCompanyCrnController {

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] =
    formProvider.conditional(
      "companyBuyerCrn.error.required",
      mappingNo = Mappings.textArea(
        "companyBuyerCrn.no.conditional.error.required",
        "companyBuyerCrn.no.conditional.error.invalid",
        "companyBuyerCrn.no.conditional.error.length"
      ),
      mappingYes = Mappings.crn(
        "companyBuyerCrn.yes.conditional.error.required",
        "companyBuyerCrn.yes.conditional.error.invalid",
        "companyBuyerCrn.yes.conditional.error.length"
      )
    )

  def viewModel(
                 srn: Srn,
                 landOrPropertyIndex: Max5000,
                 mode: Mode,
                 companyName: String
               ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "companyBuyerCrn.title",
      Message("companyBuyerCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("companyBuyerCrn.yes.conditional", companyName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("companyBuyerCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint("companyBuyerCrn.hint"),
      routes.EmployerCompanyCrnController.onSubmit(srn, landOrPropertyIndex, mode)
    )
}