package controllers.nonsipp.landorproperty

import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{IdentitySubject, IdentityType, Mode}
import navigation.Navigator
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView
import pages.nonsipp.landorproperty.RemovePropertyPage
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
class RemovePropertyController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          saveService: SaveService,
                                          @Named("non-sipp") navigator: Navigator,
                                          identifyAndRequireData: IdentifyAndRequireData,
                                          formProvider: YesNoPageFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: YesNoPageView
                                        )(implicit ec: ExecutionContext)
  extends PSRController {

  private val form = RemovePropertyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      getResult(srn, index, mode, request.userAnswers.fillForm(RemovePropertyPage(srn, index), form))
  }

  private def getResult(srn: Srn, index: Max5000, mode: Mode, form: Form[Boolean], error: Boolean = false)(
    implicit request: DataRequest[_]
  ) = {
    val whoReceivedLoanPage = request.userAnswers
      .get(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient))
    whoReceivedLoanPage match {
      case Some(who) => {
        val recipientName =
          who match {
            case IdentityType.Individual =>
              request.userAnswers.get(IndividualRecipientNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.UKCompany =>
              request.userAnswers.get(CompanyRecipientNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.UKPartnership =>
              request.userAnswers.get(PartnershipRecipientNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.Other =>
              request.userAnswers
                .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient))
                .map(_.name)
                .getOrRecoverJourney
          }
        recipientName.fold(
          l => l,
          name => {
            val loanAmount =
              request.userAnswers.get(AmountOfTheLoanPage(srn, index)).map(_._1).getOrRecoverJourney
            loanAmount.fold(
              l => l,
              amount =>
                if (error) {
                  BadRequest(view(form, viewModel(srn, index, mode, amount.displayAs, name)))
                } else {
                  Ok(view(form, viewModel(srn, index, mode, amount.displayAs, name)))
                }
            )
          }
        )
      }
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(getResult(srn, index, mode, formWithErrors, true)),
          value =>
            if (value) {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.remove(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient)))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(RemovePropertyPage(srn, index), mode, updatedAnswers))
            } else {
              Future
                .successful(Redirect(navigator.nextPage(RemovePropertyPage(srn, index), mode, request.userAnswers)))
            }
        )
  }
}

object RemovePropertyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeLoan.error.required"
  )

  def viewModel(
                 srn: Srn,
                 index: Max5000,
                 mode: Mode,
                 loanAmount: String,
                 recipientName: String
               ): FormPageViewModel[YesNoPageViewModel] =
    (
      YesNoPageViewModel(
        "removeLoan.title",
        Message("removeLoan.heading", loanAmount, recipientName),
        routes.RemovePropertyController.onSubmit(srn, index, mode)
      )
      )
}
