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
import controllers.nonsipp.routes
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
            WhatYouWillNeedPage,
            (srn, _) => routes.WhichTaxYearController.onPageLoad(srn, NormalMode),
            srn => emptyUserAnswers
          )
          .withName("go from start page to which tax year page")
      )

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedPage,
            (srn, _) => controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            srn => defaultUserAnswers
          )
          .withName("go from start page to check return dates page")
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
