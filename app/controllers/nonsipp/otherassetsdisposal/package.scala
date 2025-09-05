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

package controllers.nonsipp.otherassetsdisposal

import pages.nonsipp.otherassetsdisposal.OtherAssetsDisposalProgress
import play.api.mvc.Call
import config.RefinedTypes.{Max50, Max5000}
import models.SchemeId.Srn
import models.UserAnswers
import utils.FunctionKUtils._
import viewmodels.models.SectionJourneyStatus

import scala.concurrent.Future

implicit class CallOps(call: Call) {

  val isCyaPage: Boolean = {
    val pattern =
      s"^\\/pension-scheme-return\\/[^\\/]+\\/(change-)?check-answers-asset-disposal\\/(?!.*\\/\\d+\\/\\d+$$).*"

    call.url.matches(pattern)
  }
}

def saveProgress(
  srn: Srn,
  index: Max5000,
  secondaryIndex: Max50,
  userAnswers: UserAnswers,
  nextPage: Call,
  alwaysCompleted: Boolean = false
): Future[UserAnswers] =
  if (nextPage.isCyaPage) {
    userAnswers
      .set(
        OtherAssetsDisposalProgress(srn, index, secondaryIndex),
        SectionJourneyStatus.Completed
      )
      .mapK[Future]
  } else {
    userAnswers
      .set(
        OtherAssetsDisposalProgress(srn, index, secondaryIndex),
        if (alwaysCompleted) SectionJourneyStatus.Completed else SectionJourneyStatus.InProgress(nextPage)
      )
      .mapK[Future]
  }
