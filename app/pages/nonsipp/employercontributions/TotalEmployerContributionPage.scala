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

import utils.RefinedUtils._
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{Money, UserAnswers}
import viewmodels.models.SectionStatus
import config.Refined._
import pages.QuestionPage

import scala.util.Try

case class TotalEmployerContributionPage(srn: Srn, index: Max300, secondaryIndex: Max50) extends QuestionPage[Money] {

  override def path: JsPath =
    Paths.memberEmpContribution \ toString \ index.arrayIndex.toString \ secondaryIndex.arrayIndex.toString

  override def toString: String = "totalContribution"

  override def cleanup(value: Option[Money], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) => Try(userAnswers) // delete handled by cleanup in EmployerNamePage
      case (Some(_), None) =>
        // create
        userAnswers
          .set(EmployerContributionsSectionStatus(srn), SectionStatus.InProgress)
      case (Some(x), Some(y)) if x == y =>
        // update - no changes
        Try(userAnswers)
      case (Some(x), Some(y)) if x != y =>
        // update
        userAnswers
          .set(EmployerContributionsSectionStatus(srn), SectionStatus.InProgress)
      case _ => Try(userAnswers)
    }
}

case class TotalEmployerContributionPages(srn: Srn, index: Max300) extends QuestionPage[Map[String, Money]] {

  override def path: JsPath =
    Paths.memberEmpContribution \ toString \ index.arrayIndex.toString

  override def toString: String = "totalContribution"
}
