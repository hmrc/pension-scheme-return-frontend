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

package controllers.nonsipp.receivetransfer

import pages.nonsipp.receivetransfer.{
  DidTransferIncludeAssetPage,
  TotalValueTransferPage,
  TransferringSchemeNamePage,
  TransferringSchemeTypePage,
  TransfersInCompletedPage,
  TransfersInCompletedPages,
  WhenWasTransferReceivedPage
}
import controllers.nonsipp.receivetransfer.TransfersInCYAController._
import config.Refined._
import eu.timepit.refined.{refineMV, refineV}
import models.{NormalMode, PensionSchemeType}
import controllers.ControllerBaseSpec
import models.PensionSchemeType.PensionSchemeType
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.models.SectionCompleted
import views.html.CheckYourAnswersView

class TransfersInCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private lazy val onPageLoad = routes.TransfersInCYAController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.TransfersInCYAController.onSubmit(srn, index, NormalMode)

  private val pensionSchemeType = PensionSchemeType.Other("other")

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(TransfersInCompletedPage(srn, index, secondaryIndex), SectionCompleted)
    .unsafeSet(TransferringSchemeNamePage(srn, index, secondaryIndex), transferringSchemeName)
    .unsafeSet(TransferringSchemeTypePage(srn, index, secondaryIndex), pensionSchemeType)
    .unsafeSet(TotalValueTransferPage(srn, index, secondaryIndex), money)
    .unsafeSet(WhenWasTransferReceivedPage(srn, index, secondaryIndex), localDate)
    .unsafeSet(DidTransferIncludeAssetPage(srn, index, secondaryIndex), true)

  "TransfersInCYAController" - {

    val journeys = List(
      TransfersInCYA(
        secondaryIndex,
        transferringSchemeName,
        pensionSchemeType,
        money,
        localDate,
        transferIncludeAsset = true
      )
    )

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(srn, memberDetails.fullName, index, journeys, NormalMode)
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
