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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils._
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.OtherBuyerDetailsPage
import navigation.Navigator
import forms.RecipientDetailsFormProvider
import models.{Mode, RecipientDetails}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import controllers.nonsipp.sharesdisposal.OtherBuyerDetailsController._
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.RecipientDetailsView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RecipientDetailsViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherBuyerDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RecipientDetailsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RecipientDetailsView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form: Form[RecipientDetails] = OtherBuyerDetailsController.form(formProvider)

  def onPageLoad(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex.refined)).getOrRecoverJourney {
        companyName =>
          val form = OtherBuyerDetailsController.form(formProvider)
          Ok(
            view(
              form.fromUserAnswers(OtherBuyerDetailsPage(srn, shareIndex.refined, disposalIndex.refined)),
              viewModel(
                srn,
                shareIndex.refined,
                disposalIndex.refined,
                companyName,
                mode
              )
            )
          )
      }
    }

  def onSubmit(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex.refined)).getOrRecoverJourney {
        companyName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(
                        srn,
                        shareIndex.refined,
                        disposalIndex.refined,
                        companyName,
                        mode
                      )
                    )
                  )
                ),
              answer =>
                for {
                  updatedAnswers <- Future
                    .fromTry(
                      request.userAnswers
                        .set(OtherBuyerDetailsPage(srn, shareIndex.refined, disposalIndex.refined), answer)
                    )
                  nextPage = navigator.nextPage(
                    OtherBuyerDetailsPage(srn, shareIndex.refined, disposalIndex.refined),
                    mode,
                    updatedAnswers
                  )
                  updatedProgressAnswers <- saveProgress(
                    srn,
                    shareIndex.refined,
                    disposalIndex.refined,
                    updatedAnswers,
                    nextPage
                  )
                  _ <- saveService.save(updatedProgressAnswers)
                } yield Redirect(nextPage)
            )
      }
    }
}

object OtherBuyerDetailsController {
  def form(formProvider: RecipientDetailsFormProvider): Form[RecipientDetails] = formProvider(
    "sharesDisposal.otherBuyerDetails.name.error.required",
    "error.textarea.invalid",
    "sharesDisposal.otherBuyerDetails.name.error.length",
    "sharesDisposal.otherBuyerDetails.description.error.required",
    "error.textarea.invalid",
    "sharesDisposal.otherBuyerDetails.description.error.length"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[RecipientDetailsViewModel] =
    FormPageViewModel(
      Message("sharesDisposal.otherBuyerDetails.title"),
      Message("sharesDisposal.otherBuyerDetails.heading", companyName),
      RecipientDetailsViewModel(
        Message("sharesDisposal.otherBuyerDetails.name"),
        Message("sharesDisposal.otherBuyerDetails.description")
      ),
      routes.OtherBuyerDetailsController.onSubmit(srn, index, disposalIndex, mode)
    )
}
