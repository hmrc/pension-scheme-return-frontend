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
import models.SchemeId.Srn
import models.UserAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils.removePages
import utils.RefinedUtils._

import scala.util.Try

case class EmployerNamePage(srn: Srn, memberIndex: Max300, index: Max50) extends QuestionPage[String] {

  override def path: JsPath =
    Paths.memberEmpContribution \ toString \ memberIndex.arrayIndex.toString \ index.arrayIndex.toString

  override def toString: String = "orgName"

  override def cleanup(value: Option[String], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(_), _) => Try(userAnswers)
      case (None, _) => removePages(userAnswers, pages(srn))
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn): List[Removable[_]] = List(
    EmployerTypeOfBusinessPage(srn, memberIndex, index),
    TotalEmployerContributionPage(srn, memberIndex, index),
    EmployerCompanyCrnPage(srn, memberIndex, index),
    PartnershipEmployerUtrPage(srn, memberIndex, index),
    OtherEmployeeDescriptionPage(srn, memberIndex, index),
    ContributionsFromAnotherEmployerPage(srn, memberIndex, index)
  )

}
