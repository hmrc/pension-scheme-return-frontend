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
import models.{ConditionalYesNo, UserAnswers, Utr}
import models.SchemeId.Srn
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps
import viewmodels.models.SectionStatus

import scala.util.Try

case class PartnershipEmployerUtrPage(srn: Srn, index: Max300, secondaryIndex: Max50)
    extends QuestionPage[ConditionalYesNo[String, Utr]] {

  override def path: JsPath =
    Paths.memberEmpContribution \ toString \ index.arrayIndex.toString \ secondaryIndex.arrayIndex.toString

  override def toString: String = "idNumber"

  override def cleanup(value: Option[ConditionalYesNo[String, Utr]], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) => Try(userAnswers) // delete handled by cleanup in EmployerNamePage
      case (Some(_), None) =>
        // create
        userAnswers
          .set(EmployerContributionsSectionStatus(srn), SectionStatus.InProgress)
          .flatMap(_.remove(EmployerContributionsMemberListPage(srn)))
      case (Some(x), Some(y)) =>
        if (x != y) {
          // value updated
          userAnswers
            .set(EmployerContributionsSectionStatus(srn), SectionStatus.InProgress)
            .flatMap(_.remove(EmployerContributionsMemberListPage(srn)))
        } else {
          // value stays the same
          Try(userAnswers)
        }
      case _ => Try(userAnswers)
    }

}
