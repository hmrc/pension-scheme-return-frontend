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

package pages.nonsipp.employercontributions

import config.Refined.{Max300, Max50}
import models.{IdentityType, UserAnswers}
import models.SchemeId.Srn
import models.UserAnswers.implicits.UserAnswersTryOps
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps
import viewmodels.models.{SectionJourneyStatus, SectionStatus}

import scala.util.Try

case class EmployerTypeOfBusinessPage(srn: Srn, memberIndex: Max300, index: Max50) extends QuestionPage[IdentityType] {

  override def path: JsPath =
    Paths.memberEmpContribution \ toString \ memberIndex.arrayIndex.toString \ index.arrayIndex.toString

  override def toString: String = "orgType"

  override def cleanup(value: Option[IdentityType], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) => Try(userAnswers) // delete handled by cleanup in EmployerNamePage
      case (Some(_), None) =>
        // new value / create
        userAnswers
          .set(EmployerContributionsSectionStatus(srn), SectionStatus.InProgress)
          .flatMap(_.remove(EmployerContributionsMemberListPage(srn)))
      case (Some(a), Some(b)) if a == b => Try(userAnswers) // same answer
      case _ =>
        userAnswers
          .remove(EmployerCompanyCrnPage(srn, memberIndex, index))
          .remove(PartnershipEmployerUtrPage(srn, memberIndex, index))
          .remove(OtherEmployeeDescriptionPage(srn, memberIndex, index))
          .set(EmployerContributionsSectionStatus(srn), SectionStatus.InProgress)
          .remove(EmployerContributionsMemberListPage(srn))
    }
}
