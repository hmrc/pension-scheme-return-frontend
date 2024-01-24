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

import config.Refined.{indexReads, Max5000, OneTo5000}
import controllers.nonsipp.shares.CompanyNameOfSharesSellerPage
import eu.timepit.refined.refineMV
import models.{NormalMode, SchemeHoldShare}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.shares._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class SharesNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)

  "SharesNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeHoldAnySharesPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName(
          "go from scheme hold any shares to task list page when no is selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeHoldAnySharesPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.shares.routes.WhatYouWillNeedSharesController.onPageLoad(srn)
        )
        .withName(
          "go from scheme hold any shares to what you will need page when yes is selected"
        )
    )

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedSharesPage,
          (srn, _) => controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, index, NormalMode)
        )
        .withName(
          "go from WhatYouWillNeedSharesPage to type of shares page"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          TypeOfSharesHeldPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.WhyDoesSchemeHoldSharesController.onPageLoad(srn, index, NormalMode)
        )
        .withName(
          "go from TypeOfSharesHeldPage to WhyDoesSchemeHoldShares page"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhyDoesSchemeHoldSharesPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode),
          srn =>
            defaultUserAnswers.unsafeSet(
              WhyDoesSchemeHoldSharesPage(srn, index),
              SchemeHoldShare.Acquisition
            )
        )
        .withName(
          "go from WhyDoesSchemeHoldSharesPage to WhenDidSchemeAcquireShares page when holding is acquisition"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhyDoesSchemeHoldSharesPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode),
          srn =>
            defaultUserAnswers.unsafeSet(
              WhyDoesSchemeHoldSharesPage(srn, index),
              SchemeHoldShare.Contribution
            )
        )
        .withName(
          "go from WhyDoesSchemeHoldSharesPage to WhenDidSchemeAcquireShares page when holding is contribution"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhyDoesSchemeHoldSharesPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode),
          srn =>
            defaultUserAnswers.unsafeSet(
              WhyDoesSchemeHoldSharesPage(srn, index),
              SchemeHoldShare.Transfer
            )
        )
        .withName(
          "go from WhyDoesSchemeHoldSharesPage to CompanyNameRelatedSharesPage when holding is transfer"
        )
    )

    "WhenDidSchemeAcquireSharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireSharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from WhenDidSchemeAcquireShares to CompanyNameRelatedShares page"
          )
      )
    }

    "CompanyNameRelatedSharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CompanyNameRelatedSharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesCompanyCrnController.onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from company name related shares page to shares company Crn page"
          )
      )
    }

    "SharesCompanyCrnPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesCompanyCrnPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.ClassOfSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from shares company Crn page to class of shares page"
          )
      )
    }

    "ClassOfSharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            ClassOfSharesPage,
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from class of shares to unauthorised page")
      )
    }

    "IndividualNameOfSharesSellerPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            IndividualNameOfSharesSellerPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesIndividualSellerNINumberController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName("go from individual name of shares seller page to shares individual seller NI number page")
      )
    }

    "SharesIndividualSellerNINumberPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndividualSellerNINumberPage,
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from class of shares individual seller NI number page to unauthorised page")
      )
    }

    "CompanyNameOfSharesSellerPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CompanyNameOfSharesSellerPage,
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from  company name of shares seller page to unauthorised page")
      )
    }

  }
}
