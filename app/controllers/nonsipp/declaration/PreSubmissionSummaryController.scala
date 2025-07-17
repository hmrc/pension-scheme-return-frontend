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

package controllers.nonsipp.declaration

import services.{PsrRetrievalService, PsrVersionsService, SchemeDateService}
import play.api.mvc._
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import utils.nonsipp._
import play.api.i18n._
import viewmodels.models.{SummaryPageEntry, _}
import models.requests.DataRequest
import views.html.SummaryView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PreSubmissionSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: SummaryView,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport
    with SchemeDetailNavigationUtils {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    viewModel(srn).map(_.map(vm => Ok(view(vm, request.schemeDetails.schemeName))).merge)
  }

  def viewModel(
    srn: Srn
  )(using
    request: DataRequest[AnyContent],
    messages: Messages
  ): Future[Either[Result, FormPageViewModel[List[SummaryPageEntry]]]] = ???
}
