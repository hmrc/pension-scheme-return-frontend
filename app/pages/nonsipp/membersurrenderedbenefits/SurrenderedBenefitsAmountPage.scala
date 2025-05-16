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

package pages.nonsipp.membersurrenderedbenefits

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import pages.QuestionPage
import config.RefinedTypes.Max300
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{Money, UserAnswers}

import scala.util.Try

case class SurrenderedBenefitsAmountPage(srn: Srn, memberIndex: Max300) extends QuestionPage[Money] {

  override def path: JsPath = Paths.memberPensionSurrender \ toString \ memberIndex.arrayIndex.toString

  override def toString: String = "totalSurrendered"

  override def cleanup(value: Option[Money], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) =>
        removePages(userAnswers, pages(srn))
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn): List[Removable[_]] =
    List(
      WhenDidMemberSurrenderBenefitsPage(srn, memberIndex),
      WhyDidMemberSurrenderBenefitsPage(srn, memberIndex),
      MemberSurrenderedBenefitsProgress(srn, memberIndex)
    )
}
