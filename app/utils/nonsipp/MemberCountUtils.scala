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

package utils.nonsipp

import pages.nonsipp.schemedesignatory._
import pages.nonsipp.memberreceivedpcls.Paths.memberDetails
import utils.nonsipp.TaskListStatusUtils._
import models.SchemeId.Srn
import play.api.libs.json.JsObject
import models._
import viewmodels.models._

object MemberCountUtils {

  /**
   * Check if the current submission of the return has more than 99 members now but previously in the same submission was less than 99 members
   *
   * @param userAnswers
   * @param srn
   * @param pensionSchemeId
   * @return true if total active and deferred member number > 99 AND some other data input is present that is normally accessible from the task list
   */
  def hasMemberNumbersChangedToOver99(
    userAnswers: UserAnswers,
    srn: Srn,
    pensionSchemeId: PensionSchemeId,
    isPrePop: Boolean
  ): Boolean = {
    val noDataStatuses = List(TaskListStatus.NotStarted, TaskListStatus.UnableToStart)
    val currentSchemeMembers = userAnswers.get(HowManyMembersPage(srn, pensionSchemeId))
    lazy val memberDetailsExist = userAnswers
      .get(memberDetails)
      .getOrElse(JsObject.empty)
      .as[JsObject] != JsObject.empty
    lazy val loansExist = !noDataStatuses.contains(getLoansTaskListStatusAndLink(userAnswers, srn, isPrePop)._1)
    lazy val borrowingsExist = !noDataStatuses.contains(getBorrowingTaskListStatusAndLink(userAnswers, srn)._1)
    lazy val financialDetailsExist = !noDataStatuses.contains(
      getFinancialDetailsTaskListStatus(
        userAnswers.get(HowMuchCashPage(srn, NormalMode)),
        userAnswers.get(ValueOfAssetsPage(srn, NormalMode)),
        userAnswers.get(FeesCommissionsWagesSalariesPage(srn, NormalMode))
      )
    )
    lazy val sharesExist = !noDataStatuses.contains(getSharesTaskListStatusAndLink(userAnswers, srn, isPrePop)._1)
    lazy val landOrPropertyExist =
      !noDataStatuses.contains(getLandOrPropertyTaskListStatusAndLink(userAnswers, srn, isPrePop)._1)
    lazy val bondsExist = !noDataStatuses.contains(getBondsTaskListStatusAndLink(userAnswers, srn, isPrePop)._1)
    lazy val otherAssetsExist =
      !noDataStatuses.contains(getOtherAssetsTaskListStatusAndLink(userAnswers, srn, isPrePop)._1)

    val isCurrentlyChanging = currentSchemeMembers.exists(_.totalActiveAndDeferred > 99) && (
      memberDetailsExist ||
        loansExist ||
        borrowingsExist ||
        financialDetailsExist ||
        sharesExist ||
        landOrPropertyExist ||
        bondsExist ||
        otherAssetsExist
    )
    isCurrentlyChanging
  }
}
