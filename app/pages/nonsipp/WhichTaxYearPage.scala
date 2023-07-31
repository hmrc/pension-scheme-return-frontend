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

package pages.nonsipp

import models.{DateRange, UserAnswers}
import models.SchemeId.Srn
import config.Refined.Max3
import pages.QuestionPage
import play.api.libs.json.JsPath

import scala.util.{Success, Try}

case class WhichTaxYearPage(srn: Srn) extends QuestionPage[DateRange] { self =>
  override def path: JsPath = JsPath \ toString

  override def toString: String = "reportDetails"

  override def cleanup(value: Option[DateRange], userAnswers: UserAnswers): Try[UserAnswers] =
    if (value != userAnswers.get(self)) userAnswers.remove(CheckReturnDatesPage(srn))
    else Success(userAnswers)
}
