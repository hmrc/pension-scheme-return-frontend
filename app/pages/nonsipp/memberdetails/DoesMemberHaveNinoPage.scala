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

package pages.nonsipp.memberdetails

import config.Refined.Max99
import models.SchemeId.Srn
import models.UserAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class DoesMemberHaveNinoPage(srn: Srn, index: Max99) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString \ index.arrayIndex

  override def toString: String = "nationalInsuranceNumber"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    value match {
      case Some(true) => userAnswers.remove(NoNINOPage(srn, index))
      case Some(false) => userAnswers.remove(MemberDetailsNinoPage(srn, index))
      case None =>
        userAnswers
          .remove(NoNINOPage(srn, index))
          .flatMap(_.remove(MemberDetailsNinoPage(srn, index)))
    }
}
