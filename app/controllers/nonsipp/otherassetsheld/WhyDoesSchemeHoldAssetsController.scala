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

package controllers.nonsipp.otherassetsheld

import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.otherassetsheld.WhyDoesSchemeHoldAssetsController._
import forms.RadioListFormProvider
import models.PointOfEntry._
import models.SchemeHoldAsset._
import models.SchemeId.Srn
import models.{Mode, NormalMode, SchemeHoldAsset}
import navigation.Navigator
import pages.nonsipp.otherassetsheld.{OtherAssetsCYAPointOfEntry, WhyDoesSchemeHoldAssetsPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhyDoesSchemeHoldAssetsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = WhyDoesSchemeHoldAssetsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(WhyDoesSchemeHoldAssetsPage(srn, index)),
          viewModel(srn, index, request.schemeDetails.schemeName, mode)
        )
      )
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, index, request.schemeDetails.schemeName, mode)
                  )
                )
              ),
          answer => {
            // If in NormalMode, save answers as usual
            if (mode == NormalMode) {
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.set(WhyDoesSchemeHoldAssetsPage(srn, index), answer)
                )
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  mode,
                  updatedAnswers
                )
              )
            } else {
              // In CheckMode, before saving answers, set PointOfEntry based on the previous answer and new answer
              val previousAnswer = request.userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index))

              val pointOfEntry = (previousAnswer, answer) match {
                case (Some(Acquisition), Contribution) => Some(AssetAcquisitionToContributionPointOfEntry)
                case (Some(Acquisition), Transfer) => Some(AssetAcquisitionToTransferPointOfEntry)
                case (Some(Contribution), Acquisition) => Some(AssetContributionToAcquisitionPointOfEntry)
                case (Some(Contribution), Transfer) => Some(AssetContributionToTransferPointOfEntry)
                case (Some(Transfer), Acquisition) => Some(AssetTransferToAcquisitionPointOfEntry)
                case (Some(Transfer), Contribution) => Some(AssetTransferToContributionPointOfEntry)
                // If answer is unchanged, use NoPointOfEntry to redirect to CYA
                case (Some(_), _) => Some(NoPointOfEntry)
                case _ => None
              }

              pointOfEntry match {
                case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                case Some(updatedPointOfEntry) =>
                  for {
                    updatedAnswers <- Future.fromTry(
                      request.userAnswers
                        .set(WhyDoesSchemeHoldAssetsPage(srn, index), answer)
                        .set(OtherAssetsCYAPointOfEntry(srn, index), updatedPointOfEntry)
                    )
                    _ <- saveService.save(updatedAnswers)
                  } yield Redirect(
                    navigator.nextPage(
                      WhyDoesSchemeHoldAssetsPage(srn, index),
                      mode,
                      updatedAnswers
                    )
                  )
              }
            }
          }
        )
    }
}

object WhyDoesSchemeHoldAssetsController {

  def form(formProvider: RadioListFormProvider): Form[SchemeHoldAsset] = formProvider[SchemeHoldAsset](
    "otherAssets.whyDoesSchemeHoldAssets.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(
        Message("otherAssets.whyDoesSchemeHoldAssets.radioList1"),
        Acquisition.name,
        Message("otherAssets.whyDoesSchemeHoldAssets.radioList1.hint")
      ),
      RadioListRowViewModel(Message("otherAssets.whyDoesSchemeHoldAssets.radioList2"), Contribution.name),
      RadioListRowViewModel(Message("otherAssets.whyDoesSchemeHoldAssets.radioList3"), Transfer.name)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("otherAssets.whyDoesSchemeHoldAssets.title"),
      Message(
        "otherAssets.whyDoesSchemeHoldAssets.heading",
        schemeName
      ),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.WhyDoesSchemeHoldAssetsController.onSubmit(srn, index, mode)
    )
}
