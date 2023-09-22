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

package controllers.nonsipp

import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import models.{ManualOrUpload, Money, MoneyInPeriod, NormalMode, SchemeMemberNumbers, UserAnswers}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsNinoPage, MemberDetailsPage, NoNINOPage}
import pages.nonsipp.schemedesignatory.{
  ActiveBankAccountPage,
  FeesCommissionsWagesSalariesPage,
  HowManyMembersPage,
  HowMuchCashPage,
  ValueOfAssetsPage
}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import viewmodels.models.TaskListStatus
import viewmodels.models.TaskListStatus.TaskListStatus
import views.html.TaskListView

class TaskListControllerSpec extends ControllerBaseSpec {

  val schemeDateRange = dateRangeGen.sample.value
  val pensionSchemeId = pensionSchemeIdGen.sample.value

  private val mockSchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDateRange))
  }

  "TaskListController" - {

    lazy val viewModel = TaskListController.viewModel(
      srn,
      schemeName,
      schemeDateRange.from,
      schemeDateRange.to,
      defaultUserAnswers,
      pensionSchemeId
    )
    lazy val onPageLoad = routes.TaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "schemeDetailsSection - basic details" - {
      "inProgress" - {

        "stopped at check dates page" in {
          testViewModel(
            defaultUserAnswers,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.details.title",
            expectedLinkUrl = controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped after dates page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.details.title",
            expectedLinkUrl = controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped at active bank account page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode).url
          )

        }
        "stopped at reason no bank account page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)
              .unsafeSet(ActiveBankAccountPage(srn), false)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped at reason no bank account page when how many members already populated" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)
              .unsafeSet(ActiveBankAccountPage(srn), false)
              .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode).url
          )
        }

        "stopped at how many members page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), true)
              .unsafeSet(ActiveBankAccountPage(srn), true)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.details.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, NormalMode).url
          )
        }
      }

      "completed" in {
        val userAnswersWithHowManyMembers =
          defaultUserAnswers
            .unsafeSet(ActiveBankAccountPage(srn), true)
            .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 1, 1))

        testViewModel(
          userAnswersWithHowManyMembers,
          0,
          0,
          expectedStatus = TaskListStatus.Completed,
          expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
          expectedLinkContentKey = "nonsipp.tasklist.schemedetails.details.title",
          expectedLinkUrl =
            controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "schemeDetailsSection - financial details" - {
      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          0,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
          expectedLinkContentKey = "nonsipp.tasklist.schemedetails.add.finances.title",
          expectedLinkUrl =
            controllers.nonsipp.schemedesignatory.routes.HowMuchCashController.onPageLoad(srn, NormalMode).url
        )
      }
      "inProgress" - {

        "stopped after how much cash page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))

          testViewModel(
            userAnswersPopulated,
            0,
            1,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.finances.title",
            expectedLinkUrl =
              controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController.onPageLoad(srn, NormalMode).url
          )
        }
        "stopped after value of assets page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))
              .unsafeSet(ValueOfAssetsPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))

          testViewModel(
            userAnswersPopulated,
            0,
            1,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
            expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.finances.title",
            expectedLinkUrl = controllers.nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController
              .onPageLoad(srn, NormalMode)
              .url
          )
        }
      }

      "completed" in {
        val userAnswersWithPopulatedAnswers =
          defaultUserAnswers
            .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(Money(1), Money(2)))
            .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), Money(1))

        testViewModel(
          userAnswersWithPopulatedAnswers,
          0,
          1,
          expectedStatus = TaskListStatus.Completed,
          expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
          expectedLinkContentKey = "nonsipp.tasklist.schemedetails.change.finances.title",
          expectedLinkUrl = controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
            .onPageLoad(srn, NormalMode)
            .url
        )
      }
    }

    "membersSection" - {
      val userAnswersOneMember = defaultUserAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
        .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)
        .unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), nino)

      "notStarted" in {
        testViewModel(
          defaultUserAnswers,
          1,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "nonsipp.tasklist.members.title",
          expectedLinkContentKey = "nonsipp.tasklist.members.add.details.title",
          expectedLinkUrl = controllers.nonsipp.memberdetails.routes.PensionSchemeMembersController.onPageLoad(srn).url
        )
      }

      "inProgress" - {
        "DoesMemberHaveNinoPage is missing" in {
          val userAnswers = userAnswersOneMember
            .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.DoesSchemeMemberHaveNINOController
              .onPageLoad(srn, refineMV(2), NormalMode)
              .url
          )
        }

        "nino missing" in {
          val userAnswers = userAnswersOneMember
            .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(2)), true)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.MemberDetailsNinoController
              .onPageLoad(srn, refineMV(2), NormalMode)
              .url
          )
        }
        "no nino reason is missing" in {
          val userAnswers = userAnswersOneMember
            .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(2)), false)
            .unsafeSet(MemberDetailsPage(srn, refineMV(3)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(3)), true)
            .unsafeSet(MemberDetailsNinoPage(srn, refineMV(3)), nino)
            .unsafeSet(MemberDetailsPage(srn, refineMV(4)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(4)), false)
            .unsafeSet(NoNINOPage(srn, refineMV(4)), noninoReason)

          testViewModel(
            userAnswers,
            1,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.NoNINOController
              .onPageLoad(srn, refineMV(2), NormalMode)
              .url
          )
        }
      }

      "completed" in {
        testViewModel(
          userAnswersOneMember,
          1,
          0,
          expectedStatus = TaskListStatus.Completed,
          expectedTitleKey = "nonsipp.tasklist.members.title",
          expectedLinkContentKey = "nonsipp.tasklist.members.change.details.title",
          expectedLinkUrl = controllers.nonsipp.memberdetails.routes.SchemeMembersListController
            .onPageLoad(srn, 1, ManualOrUpload.Manual)
            .url
        )
      }
    }
  }

  private def testViewModel(
    userAnswersPopulated: UserAnswers,
    sectionIndex: Int,
    itemIndex: Int,
    expectedStatus: TaskListStatus,
    expectedTitleKey: String,
    expectedLinkContentKey: String,
    expectedLinkUrl: String
  ) = {
    val customViewModel = TaskListController.viewModel(
      srn,
      schemeName,
      schemeDateRange.from,
      schemeDateRange.to,
      userAnswersPopulated,
      pensionSchemeId
    )
    val sections = customViewModel.page.sections.toList
    sections(sectionIndex).title.key mustBe expectedTitleKey
    sections(sectionIndex).items.fold(
      _ => "",
      list => {
        val item = list.toList(itemIndex)
        item.status mustBe expectedStatus
        item.link.content.key mustBe expectedLinkContentKey
        item.link.url mustBe expectedLinkUrl
      }
    )
  }
}
