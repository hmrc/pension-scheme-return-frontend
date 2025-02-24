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

package prepop

import pages.nonsipp.memberdetails.{MemberStatus, MembersDetailsChecked, SafeToHardDelete}
import models.UserAnswers.SensitiveJsObject
import config.RefinedTypes.OneTo300
import models.SchemeId.Srn
import eu.timepit.refined.refineV
import play.api.libs.json._
import pages.nonsipp.memberdetails.Paths.personalDetails
import models.UserAnswers

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class MemberPrePopulationProcessor @Inject()() {

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson = baseUA.data.decryptedValue

    val transformedResult = baseUaJson
      .transform(personalDetails.json.pickBranch)
      .flatMap(
        _.transform(
          (personalDetails \ MemberStatus.key).json.update(
            JsPath.read[JsObject].map { statusObject =>
              JsObject(statusObject.fields.map {
                case (key, _) => key -> JsString("New")
              })
            }
          )
        )
      ) match {
      case JsSuccess(value, _) =>
        Success(currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue))))
      case _ => Try(currentUA)
    }

    val transformedResultWithCheckedFlag = transformedResult.flatMap(_.set(MembersDetailsChecked(srn), false))

    val memberStatusOptMap =
      (baseUaJson \ "membersPayments" \ "memberDetails" \ "personalDetails" \ MemberStatus.key)
        .asOpt[Map[String, String]]

    memberStatusOptMap.fold(transformedResultWithCheckedFlag)(
      memberStatusMap =>
        memberStatusMap.foldLeft(transformedResultWithCheckedFlag)((uaResult, memberStatusEntry) => {
          memberStatusEntry._1.toIntOption.flatMap(i => refineV[OneTo300](i + 1).toOption) match {
            case None => uaResult
            case Some(index) => uaResult.flatMap(_.set(SafeToHardDelete(srn, index)))
          }
        })
    )
  }
}
