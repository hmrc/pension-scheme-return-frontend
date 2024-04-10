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

package pages.nonsipp.employercontributions

import pages.QuestionPage
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import pages.nonsipp.memberpayments.MemberPaymentsPage
import viewmodels.models.SectionStatus

import scala.util.Try

case class EmployerContributionsPage(srn: Srn) extends QuestionPage[Boolean] {

  override def path: JsPath = MemberPaymentsPage.path \ toString

  override def toString: String = "employerContributionMade"

  def setSectionStatus(userAnswers: UserAnswers, newValue: Boolean): Try[UserAnswers] =
    userAnswers.set(
      EmployerContributionsSectionStatus(srn),
      if (newValue) {
        SectionStatus.InProgress
      } else {
        SectionStatus.Completed
      }
    )

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) => Try(userAnswers) // delete handled separately
      case (Some(true), Some(true)) => Try(userAnswers) // no change - do nothing
      case (Some(false), Some(false)) => Try(userAnswers) // no change - do nothing
      case (Some(x), Some(_)) =>
        // new value
        setSectionStatus(userAnswers, x)
      case (Some(x), None) =>
        // new value
        setSectionStatus(userAnswers, x)
    }
}
