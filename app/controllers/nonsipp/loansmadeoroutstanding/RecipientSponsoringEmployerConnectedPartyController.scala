/*
 * Copyright 2024 HM Revenue & Customs
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

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models._
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import pages.nonsipp.loansmadeoroutstanding._
import views.html.RadioListView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RecipientSponsoringEmployerConnectedPartyController @Inject() (
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

  val form: Form[SponsoringOrConnectedParty] = RecipientSponsoringEmployerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
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

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
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
              userAnswers <- request.userAnswers
                .set(RecipientSponsoringEmployerConnectedPartyPage(srn, index), success)
                .mapK
              nextPage = navigator
                .nextPage(RecipientSponsoringEmployerConnectedPartyPage(srn, index), mode, userAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, userAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(
              navigator.nextPage(RecipientSponsoringEmployerConnectedPartyPage(srn, index), mode, userAnswers)
            )
        )
  }

  private def recipientName(srn: Srn, index: Max5000)(implicit request: DataRequest[_]): Option[String] =
    request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient)).flatMap {
      case IdentityType.UKCompany => request.userAnswers.get(CompanyRecipientNamePage(srn, index))
      case IdentityType.UKPartnership => request.userAnswers.get(PartnershipRecipientNamePage(srn, index))
      case IdentityType.Other =>
        request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient)).map(_.name)
      case _ => None
    }
}

object RecipientSponsoringEmployerConnectedPartyController {

  def form(formProvider: RadioListFormProvider): Form[SponsoringOrConnectedParty] =
    formProvider("recipientSponsoringEmployerConnectedParty.error.required")

  def viewModel(srn: Srn, index: Max5000, recipientName: String, mode: Mode): FormPageViewModel[RadioListViewModel] =
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
