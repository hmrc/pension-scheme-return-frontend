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

package navigation.nonsipp

import play.api.mvc.Call
import pages.Page
import utils.IntUtils.toInt
import navigation.JourneyNavigator
import pages.nonsipp.membersurrenderedbenefits._
import models.{NormalMode, UserAnswers}

object SurrenderedBenefitsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ SurrenderedBenefitsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.membersurrenderedbenefits.routes.WhatYouWillNeedSurrenderedBenefitsController
          .onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedSurrenderedBenefitsPage(srn) =>
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, 1, NormalMode)

    case SurrenderedBenefitsMemberListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case SurrenderedBenefitsAmountPage(srn, memberIndex) =>
      controllers.nonsipp.membersurrenderedbenefits.routes.WhenDidMemberSurrenderBenefitsController
        .onPageLoad(srn, memberIndex, NormalMode)

    case WhenDidMemberSurrenderBenefitsPage(srn, memberIndex) =>
      controllers.nonsipp.membersurrenderedbenefits.routes.WhyDidMemberSurrenderBenefitsController
        .onPageLoad(srn, memberIndex, NormalMode)

    case WhyDidMemberSurrenderBenefitsPage(srn, memberIndex) =>
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
        .onPageLoad(srn, memberIndex, NormalMode)

    case SurrenderedBenefitsCYAPage(srn, memberIndex) =>
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, page = 1, NormalMode)

    case RemoveSurrenderedBenefitsPage(srn, _) =>
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, page = 1, NormalMode)

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      _ => {

        case SurrenderedBenefitsAmountPage(srn, memberIndex) =>
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
            .onPageLoad(srn, memberIndex, NormalMode)

        case WhenDidMemberSurrenderBenefitsPage(srn, memberIndex) =>
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
            .onPageLoad(srn, memberIndex, NormalMode)

        case WhyDidMemberSurrenderBenefitsPage(srn, memberIndex) =>
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
            .onPageLoad(srn, memberIndex, NormalMode)

        case SurrenderedBenefitsCYAPage(srn, memberIndex) =>
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
            .onPageLoad(srn, page = 1, NormalMode)

      }
}
