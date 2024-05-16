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

package controllers.nonsipp.declaration

import services._
import models.audit.PSRSubmissionEmailAuditEvent
import viewmodels.implicits._
import play.api.mvc._
import connectors.{EmailConnector, EmailStatus}
import controllers.PSRController
import _root_.config.FrontendAppConfig
import controllers.actions._
import _root_.config.Constants._
import navigation.Navigator
import uk.gov.hmrc.http.HeaderCarrier
import models.{DateRange, NormalMode, UserAnswers}
import models.requests.DataRequest
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.declaration.PsaDeclarationPage
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class PsaDeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  schemeDateService: SchemeDateService,
  view: ContentPageView,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  emailConnector: EmailConnector,
  config: FrontendAppConfig,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(view(PsaDeclarationController.viewModel(srn)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      schemeDateService.schemeDate(srn) match {
        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(dates) =>
          def emailFuture: Future[EmailStatus] =
            sendEmail(
              loggedInUserNameOrBlank(request),
              request.minimalDetails.email,
              dates,
              request.schemeDetails.schemeName
            )

          for {
            _ <- psrSubmissionService.submitPsrDetails(
              srn = srn,
              isSubmitted = true,
              fallbackCall = controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn)
            )
            _ <- emailFuture
            _ <- saveService.save(UserAnswers(request.userAnswers.id))
          } yield {
            Redirect(navigator.nextPage(PsaDeclarationPage(srn), NormalMode, request.userAnswers))
              .addingToSession((RETURN_PERIODS, schemeDateService.returnPeriodsAsJsonString(srn)))
              .addingToSession((SUBMISSION_DATE, schemeDateService.submissionDateAsString(schemeDateService.now())))
          }
      }
    }

  private def sendEmail(name: String, email: String, taxYear: DateRange, schemeName: String)(
    implicit request: DataRequest[_],
    hc: HeaderCarrier
  ): Future[EmailStatus] = {

    val requestId = hc.requestId.map(_.value).getOrElse(request.headers.get("X-Session-ID").getOrElse(""))

    val submittedDate = LocalDateTime.now().toString // TODO change as per PSR-1139

    val templateParams = Map(
      "psaName" -> name,
      "schemeName" -> schemeName,
      "taxYear" -> taxYear.toString, //TODO change as per PSR-1139
      "dateSubmitted" -> submittedDate
    )

    val reportVersion = "001" //TODO change as per PSR-1139

    emailConnector
      .sendEmail(
        PSA,
        requestId,
        psaOrPspId = request.getUserId,
        request.schemeDetails.pstr,
        email,
        config.fileReturnTemplateId,
        templateParams,
        reportVersion
      )
      .map { emailStatus =>
        auditService.sendEvent(
          PSRSubmissionEmailAuditEvent(
            schemeName = request.schemeDetails.schemeName,
            request.schemeDetails.establishers.headOption.fold(name)(e => e.name),
            psaOrPspId = request.pensionSchemeId.value,
            schemeTaxReference = request.schemeDetails.pstr,
            affinityGroup = if (request.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
            credentialRole = PSA,
            taxYear = taxYear,
            email,
            reportVersion,
            emailStatus
          )
        )
        emailStatus
      }
  }
}

object PsaDeclarationController {

  def viewModel(srn: Srn): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("psaDeclaration.title"),
      Message("psaDeclaration.heading"),
      ContentPageViewModel(),
      routes.PsaDeclarationController.onSubmit(srn)
    ).withButtonText(Message("site.agreeAndContinue"))
      .withDescription(
        ParagraphMessage("psaDeclaration.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "psaDeclaration.listItem1",
            "psaDeclaration.listItem2",
            "psaDeclaration.listItem3"
          )
      )
}
