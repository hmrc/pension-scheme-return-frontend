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

import cats.implicits.toTraverseOps
import config.Constants
import config.Refined.Max50
import models._
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.employercontributions._
import pages.nonsipp.memberpayments.EmployerContributionsPage
import play.api.mvc.Call

object EmployerContributionsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ EmployerContributionsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.employercontributions.routes.WhatYouWillNeedEmployerContributionsController
          .onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedEmployerContributionsPage(srn) =>
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, 1, NormalMode)

    case EmployerContributionsMemberListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case EmployerNamePage(srn, memberIndex, index) =>
      controllers.nonsipp.employercontributions.routes.EmployerTypeOfBusinessController
        .onPageLoad(srn, memberIndex, index, NormalMode)

    case EmployerTypeOfBusinessPage(srn, index, secondaryIndex) =>
      userAnswers.get(EmployerTypeOfBusinessPage(srn, index, secondaryIndex)) match {

        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.employercontributions.routes.EmployerCompanyCrnController
            .onPageLoad(srn, index, secondaryIndex, NormalMode)

        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
            .onPageLoad(srn, index, secondaryIndex, NormalMode)

        case Some(IdentityType.Other) =>
          controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
            .onPageLoad(srn, index, secondaryIndex, NormalMode)
      }

    case EmployerCompanyCrnPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case PartnershipEmployerUtrPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case OtherEmployeeDescriptionPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case TotalEmployerContributionPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.employercontributions.routes.ContributionsFromAnotherEmployerController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case page @ ContributionsFromAnotherEmployerPage(srn, index, _) =>
      if (userAnswers.get(page).contains(true)) {
        (
          for {
            map <- userAnswers.get(TotalEmployerContributionPages(srn, index)).getOrRecoverJourney
            indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
            nextIndex <- findNextOpenIndex[Max50.Refined](indexes).getOrRecoverJourney
          } yield controllers.nonsipp.employercontributions.routes.EmployerNameController
            .onPageLoad(srn, index, nextIndex, NormalMode)
        ).merge
      } else {
        controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
          .onPageLoad(srn, index, 1, NormalMode)
      }

    case RemoveEmployerContributionsPage(srn, memberIndex) =>
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, 1, NormalMode)

    case EmployerContributionsCYAPage(srn) =>
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, 1, NormalMode)
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    oldUserAnswers =>
      userAnswers => {

        case EmployerNamePage(srn, index, secondaryIndex) =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
            .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)

        case EmployerTypeOfBusinessPage(srn, index, secondaryIndex) =>
          (
            oldUserAnswers.get(EmployerTypeOfBusinessPage(srn, index, secondaryIndex)),
            userAnswers.get(EmployerTypeOfBusinessPage(srn, index, secondaryIndex))
          ) match {

            // same answer
            case (Some(IdentityType.UKCompany), Some(IdentityType.UKCompany)) =>
              controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)

            case (Some(IdentityType.UKPartnership), Some(IdentityType.UKPartnership)) =>
              controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)

            case (Some(IdentityType.Other), Some(IdentityType.Other)) =>
              controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)

            // different answer
            case (_, Some(IdentityType.UKCompany)) =>
              controllers.nonsipp.employercontributions.routes.EmployerCompanyCrnController
                .onPageLoad(srn, index, secondaryIndex, CheckMode)

            case (_, Some(IdentityType.UKPartnership)) =>
              controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
                .onPageLoad(srn, index, secondaryIndex, CheckMode)

            case (_, Some(IdentityType.Other)) =>
              controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
                .onPageLoad(srn, index, secondaryIndex, CheckMode)
          }

        case EmployerCompanyCrnPage(srn, index, secondaryIndex) =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
            .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)

        case PartnershipEmployerUtrPage(srn, index, secondaryIndex) =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
            .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)

        case OtherEmployeeDescriptionPage(srn, index, secondaryIndex) =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
            .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)

        case TotalEmployerContributionPage(srn, index, secondaryIndex) =>
          controllers.nonsipp.employercontributions.routes.ContributionsFromAnotherEmployerController
            .onPageLoad(srn, index, secondaryIndex, CheckMode)

        case page @ ContributionsFromAnotherEmployerPage(srn, index, secondaryIndex) =>
          if (userAnswers.get(page).contains(true)) {
            (
              for {
                map <- userAnswers.get(TotalEmployerContributionPages(srn, index)).getOrRecoverJourney
                indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
                nextIndex <- findNextOpenIndex[Max50.Refined](indexes).getOrRecoverJourney
              } yield controllers.nonsipp.employercontributions.routes.EmployerNameController
                .onPageLoad(srn, index, nextIndex, NormalMode)
            ).merge
          } else {
            controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
              .onPageLoad(srn, index, getCYAPage(secondaryIndex.value), NormalMode)
          }

        case EmployerContributionsCYAPage(srn) =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoad(srn, 1, NormalMode)
      }

  private def getCYAPage(index: Int): Int =
    Math.ceil(index.toDouble / Constants.employerContributionsCYASize).toInt
}
