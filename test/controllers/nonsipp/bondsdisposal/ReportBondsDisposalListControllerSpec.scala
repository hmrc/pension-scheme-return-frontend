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

package controllers.nonsipp.bondsdisposal

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.bondsdisposal.ReportBondsDisposalListController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{HowDisposed, NormalMode}
import pages.nonsipp.bonds.{BondsCompleted, NameOfBondsPage}
import pages.nonsipp.bondsdisposal.{BondsDisposalCompletedPage, HowWereBondsDisposedOfPage}
import viewmodels.models.SectionCompleted
import views.html.ListView

class ReportBondsDisposalListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReportBondsDisposalListController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.ReportBondsDisposalListController.onSubmit(srn, page)
  private lazy val bondsDisposalPage = routes.BondsDisposalController.onPageLoad(srn, NormalMode)

  private val page = 1
  private val bondIndexOne = refineMV[Max5000.Refined](1)
  private val disposalIndexOne = refineMV[Max50.Refined](1)
  private val disposalIndexTwo = refineMV[Max50.Refined](2)

  private val numberOfDisposals = 2
  private val maxPossibleNumberOfDisposals = 100

  private val disposalIndexes = List(disposalIndexOne, disposalIndexTwo)
  private val bondsDisposalsWithIndexes =
    List(((bondIndexOne, disposalIndexes), SectionCompleted))

  private val completedUserAnswers = defaultUserAnswers
  // Bonds data 1
    .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")
    .unsafeSet(BondsCompleted(srn, bondIndexOne), SectionCompleted)
    //Bond 1 - disposal data 1
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), HowDisposed.Sold)
    .unsafeSet(BondsDisposalCompletedPage(srn, bondIndexOne, disposalIndexOne), SectionCompleted)
    //Bond 1 - disposal data 2
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexTwo), HowDisposed.Sold)
    .unsafeSet(BondsDisposalCompletedPage(srn, bondIndexOne, disposalIndexTwo), SectionCompleted)

  "ReportBondsDisposalListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          NormalMode,
          page,
          bondsDisposalsWithIndexes,
          numberOfDisposals,
          maxPossibleNumberOfDisposals,
          completedUserAnswers
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        bondsDisposalPage,
        defaultUserAnswers
      ).withName("Not Started Journey")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
