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

package pages.nonsipp.memberreceivedpcls

import utils.PageUtils
import queries.Removable
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers
import pages.QuestionPage
import pages.nonsipp.memberreceivedpcls.Paths.membersPayments

import scala.util.Try

case class PensionCommencementLumpSumPage(srn: Srn) extends QuestionPage[Boolean] {

  override def path: JsPath = membersPayments \ toString

  override def toString: String = "lumpSumReceived"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(false), Some(true)) => PageUtils.removePages(userAnswers, pages())
      case _ => Try(userAnswers)
    }

  private def pages(): List[Removable[?]] =
    List(PensionCommencementLumpSumAmountPage.all())

}
