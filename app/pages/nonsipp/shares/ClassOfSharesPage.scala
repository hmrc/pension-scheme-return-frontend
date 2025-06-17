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

package pages.nonsipp.shares

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import pages.QuestionPage
import config.RefinedTypes.{Max5000, OneTo50}
import models.SchemeId.Srn
import eu.timepit.refined.refineV
import pages.nonsipp.sharesdisposal.{HowWereSharesDisposedPage, HowWereSharesDisposedPagesForShare}
import play.api.libs.json.JsPath
import models.UserAnswers

import scala.util.Try

case class ClassOfSharesPage(srn: Srn, index: Max5000) extends QuestionPage[String] {

  override def path: JsPath = Paths.shareIdentification \ toString \ index.arrayIndex.toString

  override def toString: String = "classOfShares"

  override def cleanup(value: Option[String], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) => // deletion
        removePages(
          userAnswers,
          dependantPages(srn, userAnswers)
        )
      case _ => Try(userAnswers) // everything else
    }

  private def dependantPages(srn: Srn, userAnswers: UserAnswers): List[Removable[_]] =
    userAnswers
      .map(HowWereSharesDisposedPagesForShare(srn, index))
      .keys
      .toList
      .flatMap(key =>
        refineV[OneTo50](key.toInt + 1)
          .fold(_ => Nil, ind => List(HowWereSharesDisposedPage(srn, index, ind)))
      )
}
