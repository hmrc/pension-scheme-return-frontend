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

import config.Refined.Max300
import models.SchemeId.Srn
import models.{NameDOB, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.{Gettable, Removable}
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class MemberDetailsPage(srn: Srn, index: Max300) extends QuestionPage[NameDOB] {

  override def path: JsPath = JsPath \ toString \ index.arrayIndex

  override def cleanup(value: Option[NameDOB], userAnswers: UserAnswers): Try[UserAnswers] =
    value.fold(userAnswers.remove(DoesMemberHaveNinoPage(srn, index)))(_ => Try(userAnswers))

  override def toString: String = "memberDetailsPage"
}

case class MembersDetailsPages(srn: Srn) extends Gettable[List[NameDOB]] with Removable[List[NameDOB]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "memberDetailsPage"
}

object MembersDetailsPages {
  implicit class MembersDetailsOps(ua: UserAnswers) {
    def membersDetails(srn: Srn): List[NameDOB] = ua.get(MembersDetailsPages(srn)).toList.flatten
  }
}

case class MemberDetails(srn: Srn) extends Gettable[Map[String, NameDOB]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "memberDetailsPage"
}
