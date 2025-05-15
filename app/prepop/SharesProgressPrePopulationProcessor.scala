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

import models.UserAnswers.SensitiveJsObject
import models.SchemeId.Srn
import pages.nonsipp.shares.Paths.sharesProgress
import play.api.libs.json._
import models.UserAnswers

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class SharesProgressPrePopulationProcessor @Inject() {

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson = baseUA.data.decryptedValue

    val transformedResult = baseUaJson.transform(sharesProgress.json.pickBranch)

    transformedResult match {
      case JsSuccess(value, _) =>
        Success(currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue))))
      case _ => Try(currentUA)
    }
  }
}
