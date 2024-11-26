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
import pages.{IndexedQuestionPage, QuestionPage}
import config.RefinedTypes.Max300
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer.{Paths => _}
import cats.syntax.bifunctor._
import play.api.libs.json.JsPath
import pages.nonsipp.membersurrenderedbenefits.{Paths => _}
import models.{NameDOB, UserAnswers}
import pages.nonsipp.membertransferout.{Paths => _}
import utils.MapUtils.UserAnswersMapOps
import cats.syntax.traverse._
import pages.nonsipp.employercontributions.{Paths => _}

case class MemberDetailsPage(srn: Srn, index: Max300) extends QuestionPage[NameDOB] {

  override def path: JsPath = MembersDetailsPage.path \ index.arrayIndex.toString

  override def toString: String = MembersDetailsPage.key
}

case class MembersDetailsPages(srn: Srn) extends IndexedQuestionPage[NameDOB] {

  override def path: JsPath = MembersDetailsPage.path

  override def toString: String = MembersDetailsPage.key
}

object MembersDetailsPage {

  val key = "nameDob"
  val path: JsPath = Paths.personalDetails \ key

  implicit class MembersDetailsOps(ua: UserAnswers) {
    def membersDetails(srn: Srn): Map[String, NameDOB] = ua.map(MembersDetailsPages(srn))

    // TODO: replace usages with completedMemberDetails as the option is not required
    def membersOptionList(srn: Srn): List[Option[NameDOB]] =
      completedMembersDetails(srn).sequence.map(_.toOption.map { case (_, details) => details })

    def completedMemberDetails(srn: Srn, index: Max300): Either[String, (Max300, NameDOB)] =
      ua.get(MemberDetailsCompletedPage(srn, index))
        .toRight("Error when refining completed members indexes")
        .flatMap { _ =>
          ua.get(MemberDetailsPage(srn, index))
            .toRight(s"Error when fetching member details page with completed index $index")
            .map(index -> _)
        }

    def completedMembersDetails(srn: Srn): Either[String, List[(Max300, NameDOB)]] =
      ua.map(MembersDetailsCompletedPages(srn))
        .refine[Max300.Refined]
        .leftMap("Error when refining completed members indexes - " + _)
        .flatMap {
          _.keys.toList.traverse { index =>
            ua.get(MemberDetailsPage(srn, index))
              .toRight(s"Error when fetching member details page with completed index $index")
              .map(index -> _)
          }
        }
  }
}
