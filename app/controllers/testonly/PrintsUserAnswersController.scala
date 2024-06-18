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

package controllers.testonly

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import play.api.libs.json.{JsObject, Json}
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class PrintsUserAnswersController @Inject()(
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
) extends FrontendBaseController
    with I18nSupport {

  def printUserAnswers(srn: Srn, key: Option[String] = None): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      key match {
        case Some(k) =>
          request.userAnswers.data.decryptedValue.fields
            .find { case (key, _) => key == k }
            .fold(BadRequest(s"key $k does not exist in user answers"))(
              field => Ok(Json.prettyPrint(JsObject(Seq(field))))
            )
        case None => Ok(Json.prettyPrint(request.userAnswers.data.decryptedValue))
      }
    }
}
