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

package pages.nonsipp.memberdetails

import utils.RefinedUtils.RefinedIntOps
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import config.Refined.Max300
import pages.QuestionPage

import scala.util.Try

case class DoesMemberHaveNinoPage(srn: Srn, index: Max300) extends QuestionPage[Boolean] {

  override def path: JsPath = Paths.personalDetails \ toString \ index.arrayIndex.toString

  override def toString: String = "nationalInsuranceNumber"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    value match {
      case Some(true) => userAnswers.removePages(List(NoNINOPage(srn, index), MemberDetailsCompletedPage(srn, index)))
      case Some(false) =>
        userAnswers.removePages(List(MemberDetailsNinoPage(srn, index), MemberDetailsCompletedPage(srn, index)))
      case None =>
        userAnswers.removePages(
          List(NoNINOPage(srn, index), MemberDetailsNinoPage(srn, index), MemberDetailsCompletedPage(srn, index))
        )
    }
}
