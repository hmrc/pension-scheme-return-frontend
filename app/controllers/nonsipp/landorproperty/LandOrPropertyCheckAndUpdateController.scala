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

package controllers.nonsipp.landorproperty

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.nonsipp.summary.LandOrPropertyCheckAnswersUtils
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import models._
import play.api.i18n.MessagesApi
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.PrePopCheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

class LandOrPropertyCheckAndUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: PrePopCheckYourAnswersView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    LandOrPropertyCheckAnswersUtils
      .landOrPropertySummaryData(srn, index, NormalMode)
      .map { data =>
        val sections = LandOrPropertyCheckAnswersUtils
          .viewModel(
            data.srn,
            data.index,
            data.schemeName,
            data.landOrPropertyInUk,
            data.landRegistryTitleNumber,
            data.holdLandProperty,
            data.landOrPropertyAcquire,
            data.landOrPropertyTotalCost,
            data.landPropertyIndependentValuation,
            data.receivedLandType,
            data.recipientName,
            data.recipientDetails,
            data.recipientReasonNoDetails,
            data.landOrPropertySellerConnectedParty,
            data.landOrPropertyResidential,
            data.landOrPropertyLease,
            data.landOrPropertyTotalIncome,
            data.addressLookUpPage,
            data.leaseDetails,
            data.mode,
            data.viewOnlyUpdated,
            data.optYear,
            data.optCurrentVersion,
            data.optPreviousVersion
          )
          .page
          .sections
        Ok(
          view(
            LandOrPropertyCheckAndUpdateController.viewModel(
              data.srn,
              data.index,
              sections
            )
          )
        )
      }
      .merge
  }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { _ =>
    Redirect(routes.IsLandOrPropertyResidentialController.onPageLoad(srn, index, NormalMode))
  }
}

object LandOrPropertyCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Max5000,
    sections: List[CheckYourAnswersSection]
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = NormalMode,
      title = "landOrPropertyCheckAndUpdate.title",
      heading = "landOrPropertyCheckAndUpdate.heading",
      description = Some("landOrPropertyCheckAndUpdate.description"),
      page = CheckYourAnswersViewModel(sections),
      refresh = None,
      buttonText = "landOrPropertyCheckAndUpdate.button",
      details = None,
      onSubmit = routes.LandOrPropertyCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
}
