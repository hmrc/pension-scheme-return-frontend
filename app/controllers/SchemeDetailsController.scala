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

package controllers

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions._
import models.{Establisher, EstablisherKind, NormalMode, SchemeDetails}
import models.SchemeId.Srn
import navigation.Navigator
import pages.SchemeDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ListUtils._
import viewmodels.ComplexMessageElement.{LinkedMessage, Message}
import viewmodels.{Delimiter, DisplayMessage}
import viewmodels.DisplayMessage.{ComplexMessage, SimpleMessage}
import viewmodels.models.ContentTablePageViewModel
import views.html.ContentTablePageView
import viewmodels.implicits._

class SchemeDetailsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         allowAccess: AllowAccessActionProvider,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         createData: DataCreationAction,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: ContentTablePageView,
                                         config: FrontendAppConfig
                                       ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request => Ok(view(viewModel(srn, request.request.schemeDetails)))
  }

  def onSubmit(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen createData) {
    implicit request =>
      Redirect(navigator.nextPage(SchemeDetailsPage(srn), NormalMode, request.userAnswers))
  }

  private val linkUrl = s"${config.urls.managePensionsSchemes.baseUrl}/overview"

  protected[controllers] def viewModel(srn: Srn, schemeDetails: SchemeDetails): ContentTablePageViewModel = {

    val schemeEstablisherNameRow: Option[(SimpleMessage, SimpleMessage)] =
      schemeDetails.establishers.headOption.map(establisher => ("schemeDetails.row4",  establisher.name))

    val otherSchemeEstablisherNameRows: Option[(DisplayMessage, DisplayMessage)] = schemeDetails.establishers match {
      case _ :: Nil => None
      case _ :: others =>
        Some(("schemeDetails.row5", ComplexMessage(others.map(other => Message(other.name)), Delimiter.Newline)))
    }

    ContentTablePageViewModel(
      title = "schemeDetails.title",
      heading = "schemeDetails.heading",
      inset = ComplexMessage(Message("schemeDetails.inset"), LinkedMessage("schemeDetails.inset.link", linkUrl)),
      buttonText = "site.saveAndContinue",
      routes.SchemeDetailsController.onSubmit(srn),
      List(
        "schemeDetails.row1" -> schemeDetails.pstr,
        "schemeDetails.row2" -> schemeDetails.schemeName,
        "schemeDetails.row3" -> schemeDetails.schemeType.capitalize
      ).toSimpleMessages :?+ schemeEstablisherNameRow :++ otherSchemeEstablisherNameRows: _*
    )
  }
}
