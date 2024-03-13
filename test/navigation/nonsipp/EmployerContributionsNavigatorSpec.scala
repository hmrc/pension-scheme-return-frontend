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

package navigation.nonsipp

import config.Refined.{Max300, Max50}
import eu.timepit.refined.{refineMV, refineV}
import models.SchemeId.Srn
import models.{IdentityType, NormalMode, UserAnswers}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.employercontributions._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps
import viewmodels.models.SectionJourneyStatus

class EmployerContributionsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

  "EmployerContributionsNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          EmployerContributionsPage,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.employercontributions.routes.WhatYouWillNeedEmployerContributionsController
              .onPageLoad(srn)
        )
        .withName(
          "go from employer contribution page to what you will need employer contributions page when yes selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithData(
          EmployerContributionsPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from employer contribution page to task list page when no selected")
    )

  }

  "WhatYouWillNeedEmployerContributionsPage" - {
    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedEmployerContributionsPage,
          (srn, _) =>
            controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from what you will need employer contributions page to employer contributions list page ")
    )
  }

  "EmployerContributionsMemberListPage" - {
    act.like(
      normalmode
        .navigateTo(
          EmployerContributionsMemberListPage,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from employer contribution page to task list page")
    )
  }

  "EmployerNamePage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          EmployerNamePage,
          (srn, index: Max300, secondaryIndex: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.EmployerTypeOfBusinessController
              .onPageLoad(srn, index, secondaryIndex, NormalMode)
        )
        .withName("go from employer name page to employer type of business page")
    )

    def userAnswersWithEmployerNames(num: Int)(srn: Srn): UserAnswers =
      (1 to num).foldLeft(defaultUserAnswers) { (ua, i) =>
        val secondaryIndex =
          refineV[Max50.Refined](i).getOrElse(throw new RuntimeException(s"$i was not in between 1 and 50"))
        ua.unsafeSet(EmployerNamePage(srn, refineMV[Max300.Refined](1), secondaryIndex), "test employer name")
      }

    List(
      (1, 1),
      (5, 1),
      (6, 2),
      (10, 2),
      (12, 3),
      (49, 10)
    ).foreach {
      case (navigatingFromIndex, expectedPage) =>
        val userAnswers = userAnswersWithEmployerNames(50) _
        val secondaryIndex =
          refineV[Max50.Refined](navigatingFromIndex)
            .getOrElse(throw new RuntimeException(s"$index was not in between 1 and 50"))
        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              index,
              secondaryIndex,
              EmployerNamePage,
              (srn, index: Max300, _: Max50, _) =>
                controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                  .onPageLoad(srn, index, expectedPage, NormalMode),
              userAnswers
            )
            .withName(
              s"go from employer name page to employer contributions CYA page $expectedPage when navigating from page with index $navigatingFromIndex"
            )
        )
    }
  }

  "EmployerTypeOfBusinessPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          secondaryIndex,
          EmployerTypeOfBusinessPage,
          Gen.const(IdentityType.UKCompany),
          (srn, memberIndex: Max300, index: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.EmployerCompanyCrnController
              .onPageLoad(srn, memberIndex, index, NormalMode)
        )
        .withName("go from employer type of business page to unauthorised")
    )

    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          secondaryIndex,
          EmployerTypeOfBusinessPage,
          Gen.const(IdentityType.UKPartnership),
          (srn, memberIndex: Max300, index: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
              .onPageLoad(srn, memberIndex, index, NormalMode)
        )
        .withName("go from employer type of business page to unauthorised page")
    )

    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          secondaryIndex,
          EmployerTypeOfBusinessPage,
          Gen.const(IdentityType.Other),
          (srn, memberIndex: Max300, index: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
              .onPageLoad(srn, memberIndex, index, NormalMode)
        )
        .withName("go from employer type of business page to unauthorised controller page")
    )
  }

  "OtherEmployeeDescriptionPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          OtherEmployeeDescriptionPage,
          controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController.onPageLoad
        )
        .withName("go from OtherEmployeeDescriptionPage to TotalEmployerContribution page")
    )

  }

  "PartnershipEmployerUtrPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          PartnershipEmployerUtrPage,
          (srn, index: Max300, secondaryIndex: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
              .onPageLoad(srn, index, secondaryIndex, NormalMode)
        )
        .withName("go from partnership employer utr page to unauthorised page")
    )
  }

  "TotalEmployerContributionPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          TotalEmployerContributionPage,
          (srn, index: Max300, secondaryIndex: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.ContributionsFromAnotherEmployerController
              .onPageLoad(srn, index, secondaryIndex, NormalMode)
        )
        .withName("go from TotalEmployerContributionPage to contribution from another employer page")
    )
  }

  "EmployerCompanyCrnPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          EmployerCompanyCrnPage,
          (srn, index: Max300, secondaryIndex: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
              .onPageLoad(srn, index, secondaryIndex, NormalMode)
        )
        .withName("go from employer company crn page to total employer contribution page")
    )
  }

  "ContributionsFromAnotherEmployerPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          ContributionsFromAnotherEmployerPage,
          (srn, index: Max300, _: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
              .onPageLoad(srn, index, page = 1, NormalMode)
        )
        .withName("go from contribution from another employer page to unauthorised page")
    )

    List(
      (List("0"), refineMV[Max50.Refined](2)),
      (List("0", "1", "2"), refineMV[Max50.Refined](4)),
      (List("1", "2"), refineMV[Max50.Refined](1)), // deleted first entry
      (List("0", "1", "3"), refineMV[Max50.Refined](3)), // deleted one entry in the middle
      (List("0", "1", "2", "5", "6"), refineMV[Max50.Refined](4)), // deleted two entry in the middle
      (List("0", "1", "3", "5", "6"), refineMV[Max50.Refined](3)) // deleted entry in the middle of two sections
    ).foreach {
      case (existingIndexes, expectedRedirectIndex) =>
        def userAnswers(srn: Srn) =
          defaultUserAnswers
            .unsafeSet(ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex), true)
            .unsafeSet(TotalEmployerContributionPages(srn, index), existingIndexes.map(_ -> money).toMap)
            .unsafeSet(
              EmployerContributionsProgress.all(srn, index),
              existingIndexes.map(_ -> SectionJourneyStatus.Completed).toMap
            )

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              index,
              secondaryIndex,
              ContributionsFromAnotherEmployerPage,
              (srn, index: Max300, _: Max50, _) =>
                controllers.nonsipp.employercontributions.routes.EmployerNameController
                  .onPageLoad(srn, index, expectedRedirectIndex, NormalMode),
              userAnswers
            )
            .withName(
              s"go from contribution from another employer page to employer name page with index ${expectedRedirectIndex.value} when indexes $existingIndexes already exist"
            )
        )
    }
  }

  "RemoveEmployerContributionsPage" - {
    "with no employer names in user answers" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            RemoveEmployerContributionsPage,
            (srn, _: Max300, _) =>
              controllers.nonsipp.employercontributions.routes.EmployerContributionsController
                .onPageLoad(srn, NormalMode)
          )
          .withName("go from remove employer page to were there any employer contributions page")
      )
    }
    "with employer names in user answers" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            RemoveEmployerContributionsPage,
            (srn, _: Max300, _) =>
              controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
                .onPageLoad(srn, 1, NormalMode),
            srn => defaultUserAnswers.unsafeSet(EmployerNamePage(srn, refineMV(1), refineMV(1)), employerName)
          )
          .withName("go from remove employer page to contributions list page")
      )
    }
  }

  "EmployerContributionsCYAPage" - {
    act.like(
      normalmode
        .navigateTo(
          EmployerContributionsCYAPage,
          (srn, _) =>
            controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
              .onPageLoad(srn, page = 1, NormalMode)
        )
        .withName("go from EmployerContributionsCYAPage to ??? page")
    )
  }
}
