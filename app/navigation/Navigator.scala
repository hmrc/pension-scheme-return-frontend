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

import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import play.api.mvc.Call
import pages._
import models.SchemeId.Srn
import controllers.nonsipp.routes
import pages.nonsipp.accountingperiod.AccountingPeriods
import utils.nonsipp.TaskListStatusUtils
import models._
import viewmodels.models.TaskListStatus
import models.requests.DataRequest
import pages.nonsipp.CheckReturnDatesPage
import play.api.libs.json.JsObject

import javax.inject.{Inject, Singleton}

@Singleton
class RootNavigator @Inject() extends Navigator {

  val journeys: List[JourneyNavigator] =
    List(new JourneyNavigator {
      override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
        case WhatYouWillNeedPage(srn) =>
          navigateFromStartPage(userAnswers, srn)

        case CheckUpdateInformationPage(srn) =>
          navigateFromStartPage(userAnswers, srn)
      }

      override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
        _ => _ => PartialFunction.empty
    })

  private def navigateFromStartPage(userAnswers: UserAnswers, srn: Srn): Call = {
    val isDataEmpty = userAnswers.data.decryptedValue == JsObject.empty
    val isCheckReturnDatesPageEmpty = userAnswers.get(CheckReturnDatesPage(srn)).isEmpty
    if (isDataEmpty || isCheckReturnDatesPageEmpty || isBasicDetailsIncomplete(srn, userAnswers)) {
      routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
    } else {
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
    }
  }

  override def defaultNormalMode: Call = controllers.routes.IndexController.onPageLoad()

  override def defaultCheckMode: Call = controllers.routes.IndexController.onPageLoad()

  private def isBasicDetailsIncomplete(srn: Srn, userAnswers: UserAnswers): Boolean =
    TaskListStatusUtils.getBasicDetailsTaskListStatus(
      userAnswers.get(CheckReturnDatesPage(srn)),
      userAnswers.get(AccountingPeriods(srn)),
      userAnswers.get(ActiveBankAccountPage(srn)),
      userAnswers.get(WhyNoBankAccountPage(srn)),
      userAnswers.get(HowManyMembersPage.bySrn())
    ) == TaskListStatus.InProgress
}

trait Navigator {
  def journeys: List[JourneyNavigator]
  def defaultNormalMode: Call
  def defaultCheckMode: Call

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit req: DataRequest[?]): Call = mode match {

    case NormalMode =>
      journeys
        .foldLeft(PartialFunction.empty[Page, Call])((acc, curr) => acc.orElse(curr.normalRoutes(userAnswers)))
        .lift(page)
        .getOrElse(defaultNormalMode)

    case CheckMode =>
      journeys
        .foldLeft(PartialFunction.empty[Page, Call])((acc, curr) =>
          acc.orElse(curr.checkRoutes(req.userAnswers)(userAnswers))
        )
        .lift(page)
        .getOrElse(defaultCheckMode)

    case ViewOnlyMode =>
      controllers.routes.JourneyRecoveryController.onPageLoad()
  }
}
