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

package navigation

import utils.BaseSpec
import pages._
import pages.nonsipp.CheckReturnDatesPage
import models.NormalMode

class NavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator = new RootNavigator

  "Navigator" - {

    "NormalMode" - {

      act.like(
        normalmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad())
          .withName("redirect any unknown pages to index page")
      )

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedPage.apply,
            (srn, _) => controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            _ => emptyUserAnswers
          )
          .withName("go from start page to check return dates page when answers are empty")
      )

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedPage.apply,
            (srn, _) => controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            _ => defaultUserAnswers
          )
          .withName(
            "go from start page to check return dates page when answers are non empty but check dates returns page is empty"
          )
      )

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedPage.apply,
            (srn, _) => controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            srn => defaultUserAnswers.set(CheckReturnDatesPage(srn), true).get
          )
          .withName(
            "go from start page to check return dates page when answers are non empty but basic details are incomplete"
          )
      )

      act.like(
        normalmode
          .navigateTo(
            CheckUpdateInformationPage.apply,
            (srn, _) => controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            _ => emptyUserAnswers
          )
          .withName("go from check update information page to check return dates page when answers are empty")
      )

      act.like(
        normalmode
          .navigateTo(
            CheckUpdateInformationPage.apply,
            (srn, _) => controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            _ => defaultUserAnswers
          )
          .withName(
            "go from update information page to check return dates page when answers are non empty but check dates returns page is empty"
          )
      )

      act.like(
        normalmode
          .navigateTo(
            CheckUpdateInformationPage.apply,
            (srn, _) => controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            srn => defaultUserAnswers.set(CheckReturnDatesPage(srn), true).get
          )
          .withName(
            "go from update information page to check return dates page when answers are non empty but basic details are incomplete"
          )
      )
    }

    "CheckMode" - {

      act.like(
        normalmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad())
          .withName("redirect any unknown pages to index page")
      )
    }
  }
}
