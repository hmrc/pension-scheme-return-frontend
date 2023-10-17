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

package controllers.nonsipp.moneyborrowed

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.moneyborrowed.MoneyBorrowedCYAController._
import eu.timepit.refined.refineMV
import models.CheckOrChange
import pages.nonsipp.moneyborrowed._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{PsrSubmissionService, SchemeDateService}
import views.html.CheckYourAnswersView

class MoneyBorrowedCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockSchemeDateService, mockPsrSubmissionService)

  private val index = refineMV[OneTo5000](1)
  private val taxYear = Some(Left(dateRange))

  private def onPageLoad(checkOrChange: CheckOrChange) =
    routes.MoneyBorrowedCYAController.onPageLoad(srn, index, checkOrChange)

  private def onSubmit(checkOrChange: CheckOrChange) = routes.MoneyBorrowedCYAController.onSubmit(srn, checkOrChange)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(LenderNamePage(srn, index), lenderName)
    .unsafeSet(IsLenderConnectedPartyPage(srn, index), true)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, index), localDate)
    .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), money)
    .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index), schemeName)

  "MoneyBorrowedCYAController" - {

    List(CheckOrChange.Check, CheckOrChange.Change).foreach { checkOrChange =>
      act.like(
        renderView(onPageLoad(checkOrChange), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                lenderName,
                lenderConnectedParty = true,
                borrowedAmountAndRate = (money, percentage),
                whenBorrowed = localDate,
                schemeAssets = money,
                schemeBorrowed = schemeName,
                checkOrChange
              )
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct ${checkOrChange.name} view")
      )

      act.like(
        redirectNextPage(onSubmit(checkOrChange))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .withName(s"redirect to next page when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(checkOrChange))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(checkOrChange))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${checkOrChange.name} mode")
      )
    }
  }
}
