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

package controllers.nonsipp.otherassetsheld

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.otherassetsheld.{
  OtherAssetsCYAPointOfEntry,
  WhenDidSchemeAcquireAssetsPage,
  WhyDoesSchemeHoldAssetsPage
}
import models.PointOfEntry._
import controllers.nonsipp.otherassetsheld.WhyDoesSchemeHoldAssetsController._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import models.{Mode, NormalMode, SchemeHoldAsset}
import models.SchemeHoldAsset._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhyDoesSchemeHoldAssetsController @Inject() (
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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(WhyDoesSchemeHoldAssetsPage(srn, index)),
          viewModel(srn, index, request.schemeDetails.schemeName, mode)
        )
      )
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
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
          answer =>
            // If in NormalMode, save answers as usual
            if (mode == NormalMode) {
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.set(WhyDoesSchemeHoldAssetsPage(srn, index), answer)
                )
                nextPage = navigator.nextPage(WhyDoesSchemeHoldAssetsPage(srn, index), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
            } else {
              // In CheckMode, before saving answers, set PointOfEntry based on the previous answer and new answer
              val previousAnswer = request.userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index))
              val previousPointOfEntry = request.userAnswers.get(OtherAssetsCYAPointOfEntry(srn, index))
              val whenDidSchemeAcquireAssetsPage = request.userAnswers.get(WhenDidSchemeAcquireAssetsPage(srn, index))

              val pointOfEntry = (previousAnswer, answer, whenDidSchemeAcquireAssetsPage) match {
                case (Some(Acquisition), Contribution, Some(_)) => Some(AssetAcquisitionToContributionPointOfEntry)
                case (Some(Acquisition), Contribution, None) => Some(WhenDidSchemeAcquireAssetsPointOfEntry)
                case (Some(Acquisition), Transfer, _) => Some(AssetAcquisitionToTransferPointOfEntry)
                case (Some(Contribution), Acquisition, Some(_)) => Some(AssetContributionToAcquisitionPointOfEntry)
                case (Some(Contribution), Acquisition, None) => Some(WhenDidSchemeAcquireAssetsPointOfEntry)
                case (Some(Contribution), Transfer, _) => Some(AssetContributionToTransferPointOfEntry)
                case (Some(Transfer), Acquisition, _) => Some(AssetTransferToAcquisitionPointOfEntry)
                case (Some(Transfer), Contribution, _) => Some(AssetTransferToContributionPointOfEntry)
                // If answer is unchanged, use previousPointOfEntry. If this is NoPointOfEntry then redirect to CYA.
                case (Some(_), _, _) => previousPointOfEntry
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
                    nextPage = navigator.nextPage(WhyDoesSchemeHoldAssetsPage(srn, index), mode, updatedAnswers)
                    updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
                    _ <- saveService.save(updatedProgressAnswers)
                  } yield Redirect(nextPage)
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
