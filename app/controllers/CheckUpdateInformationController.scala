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

package controllers

import pages.nonsipp.schemedesignatory.HowManyMembersPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.CheckUpdateInformationPage
import config.FrontendAppConfig
import controllers.actions._
import navigation.Navigator
import models.{CheckMode, NormalMode}
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import scala.concurrent.Future

import javax.inject.{Inject, Named}

class CheckUpdateInformationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      val dashboardUrl =
        if (request.pensionSchemeId.isPSP) {
          config.urls.managePensionsSchemes.schemeSummaryPSPDashboard(srn)
        } else {
          config.urls.managePensionsSchemes.schemeSummaryDashboard(srn)
        }

      Ok(
        view(
          CheckUpdateInformationController.viewModel(
            srn,
            request.schemeDetails.schemeName,
            dashboardUrl
          )
        )
      )
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      // as we cannot access pensionSchemeId in the navigator
      val members = request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId))
      if (members.exists(_.totalActiveAndDeferred > 99)) {
        Future.successful(
          Redirect(controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode))
        )
      } else {
        Future
          .successful(Redirect(navigator.nextPage(CheckUpdateInformationPage(srn), NormalMode, request.userAnswers)))
      }
    }
}

object CheckUpdateInformationController {

  def viewModel(srn: Srn, schemeName: String, dashboardUrl: String): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("checkUpdateInformation.title"),
      Message("checkUpdateInformation.heading"),
      ContentPageViewModel(isStartButton = true),
      routes.CheckUpdateInformationController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("checkUpdateInformation.paragraph1") ++
          ParagraphMessage("checkUpdateInformation.paragraph2") ++
          Heading2.medium("checkUpdateInformation.h2") ++
          ParagraphMessage("checkUpdateInformation.paragraph3")
      )
      .withBreadcrumbs(
        List(
          schemeName -> dashboardUrl,
          "checkUpdateInformation.breadcrumb.overview" -> controllers.routes.OverviewController.onPageLoad(srn).url,
          "checkUpdateInformation.title" -> "#"
        )
      )
}
