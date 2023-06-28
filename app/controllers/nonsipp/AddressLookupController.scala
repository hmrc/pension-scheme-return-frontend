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

package controllers.nonsipp

import connectors.AddressLookupConnector
import controllers.actions._
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.CompanyRecipientNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}
import views.html.TextInputView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  addressLookupConnector: AddressLookupConnector,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val continueUrl = routes.AddressLookupController.onSubmit(srn).absoluteURL()
    for {
      redirectUrl <- addressLookupConnector.init(continueUrl)
    } yield Redirect(redirectUrl)
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val addressLookupId = request.queryString.get("id").flatMap(_.headOption)
//    for {
//      updatedAnswers <- Future.fromTry(request.userAnswers.set(CompanyRecipientNamePage(srn), answer))
//      _ <- saveService.save(updatedAnswers)
//    } yield
    Ok(addressLookupId.getOrElse("nope"))
  }
}
