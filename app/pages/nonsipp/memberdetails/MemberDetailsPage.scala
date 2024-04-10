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
import queries.{Gettable, Removable}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{NameDOB, UserAnswers}
import config.Refined.Max300
import pages.QuestionPage

import scala.util.Try

case class MemberDetailsPage(srn: Srn, index: Max300) extends QuestionPage[NameDOB] {

  // won't work with a get all pages as Map as the arrayIndex must be a string
  override def path: JsPath = Paths.personalDetails \ toString \ index.arrayIndex

  override def cleanup(value: Option[NameDOB], userAnswers: UserAnswers): Try[UserAnswers] =
    value.fold(userAnswers.remove(DoesMemberHaveNinoPage(srn, index)))(_ => Try(userAnswers))

  override def toString: String = "nameDob"
}

case class MembersDetailsPages(srn: Srn) extends Gettable[List[NameDOB]] with Removable[List[NameDOB]] {

  override def path: JsPath = Paths.personalDetails \ toString

  override def toString: String = "nameDob"
}

object MembersDetailsPages {
  implicit class MembersDetailsOps(ua: UserAnswers) {
    def membersDetails(srn: Srn): List[NameDOB] = ua.list(MembersDetailsPages(srn))
  }
}
