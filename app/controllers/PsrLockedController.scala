/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import play.api.Logging
import views.html.PsrLockedView
import models.SchemeId.Srn
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class PsrLockedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig,
  identify: IdentifierAction,
  view: PsrLockedView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify { implicit request =>
      Ok(view(controllers.routes.OverviewController.onPageLoad(srn).url, config.reportAProblemUrl))
    }
}
