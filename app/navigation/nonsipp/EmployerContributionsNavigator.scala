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

import eu.timepit.refined.refineMV
import models.{IdentityType, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.employercontributions._
import pages.nonsipp.memberpayments.EmployerContributionsPage
import play.api.mvc.Call

object EmployerContributionsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case TotalEmployerContributionPage(srn, index, secondaryIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case OtherEmployeeDescriptionPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case page @ EmployerContributionsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.employercontributions.routes.WhatYouWillNeedEmployerContributionsController
          .onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case EmployerNamePage(srn, memberIndex, index) =>
      controllers.nonsipp.employercontributions.routes.EmployerTypeOfBusinessController
        .onPageLoad(srn, memberIndex, index, NormalMode)

    case EmployerTypeOfBusinessPage(srn, memberIndex, index) =>
      userAnswers.get(EmployerTypeOfBusinessPage(srn, memberIndex, index)) match {

        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.employercontributions.routes.EmployerCompanyCrnController
            .onPageLoad(srn, memberIndex, index, NormalMode)

        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
            .onPageLoad(srn, memberIndex, index, NormalMode)

        case Some(IdentityType.Other) =>
          controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
            .onPageLoad(srn, memberIndex, index, NormalMode)
      }

    case EmployerCompanyCrnPage(srn, memberIndex, index) => controllers.routes.UnauthorisedController.onPageLoad()

    case PartnershipEmployerUtrPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case WhatYouWillNeedEmployerContributionsPage(srn) =>
      controllers.nonsipp.employercontributions.routes.EmployerNameController
        .onPageLoad(srn, refineMV(1), refineMV(2), NormalMode)

  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
}
