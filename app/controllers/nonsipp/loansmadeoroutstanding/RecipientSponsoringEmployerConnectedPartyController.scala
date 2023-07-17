/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.Max9999999
import controllers.actions._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, ReceivedLoanType, SponsoringOrConnectedParty}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{
  CompanyRecipientNamePage,
  IndividualRecipientNamePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientNamePage,
  RecipientSponsoringEmployerConnectedPartyPage,
  WhoReceivedLoanPage
}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RecipientSponsoringEmployerConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = RecipientSponsoringEmployerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      recipientName(srn, index)
        .map { recipientName =>
          Ok(
            view(
              form.fromUserAnswers(RecipientSponsoringEmployerConnectedPartyPage(srn, index)),
              RecipientSponsoringEmployerConnectedPartyController.viewModel(srn, index, recipientName, mode)
            )
          )
        }
        .getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            recipientName(srn, index)
              .map { recipientName =>
                Future.successful(
                  BadRequest(
                    view(
                      errors,
                      RecipientSponsoringEmployerConnectedPartyController.viewModel(srn, index, recipientName, mode)
                    )
                  )
                )
              }
              .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))),
          success =>
            for {
              userAnswers <- Future
                .fromTry(request.userAnswers.set(RecipientSponsoringEmployerConnectedPartyPage(srn, index), success))
              _ <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(RecipientSponsoringEmployerConnectedPartyPage(srn, index), mode, userAnswers))
            }
        )
  }

  private def recipientName(srn: Srn, index: Max9999999)(implicit request: DataRequest[_]): Option[String] =
    request.userAnswers.get(WhoReceivedLoanPage(srn, refineMV(1))).flatMap {
      case ReceivedLoanType.UKCompany => request.userAnswers.get(CompanyRecipientNamePage(srn, index))
      case ReceivedLoanType.UKPartnership => request.userAnswers.get(PartnershipRecipientNamePage(srn, index))
      case ReceivedLoanType.Other => request.userAnswers.get(OtherRecipientDetailsPage(srn, index)).map(_.name)
      case _ => None
    }
}

object RecipientSponsoringEmployerConnectedPartyController {

  def form(formProvider: RadioListFormProvider): Form[SponsoringOrConnectedParty] =
    formProvider("recipientSponsoringEmployerConnectedParty.error.required")

  def viewModel(srn: Srn, index: Max9999999, recipientName: String, mode: Mode): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "recipientSponsoringEmployerConnectedParty.title",
      Message("recipientSponsoringEmployerConnectedParty.heading", recipientName),
      List(
        RadioListRowViewModel(
          "recipientSponsoringEmployerConnectedParty.option1",
          SponsoringOrConnectedParty.Sponsoring.name
        ),
        RadioListRowViewModel(
          "recipientSponsoringEmployerConnectedParty.option2",
          SponsoringOrConnectedParty.ConnectedParty.name
        ),
        RadioListRowDivider.Or,
        RadioListRowViewModel(
          "recipientSponsoringEmployerConnectedParty.option3",
          SponsoringOrConnectedParty.Neither.name
        )
      ),
      routes.RecipientSponsoringEmployerConnectedPartyController.onSubmit(srn, index, mode)
    )
}
