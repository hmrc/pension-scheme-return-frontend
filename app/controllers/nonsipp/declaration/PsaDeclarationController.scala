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
import utils.DateTimeUtils
import play.api.mvc._
import connectors.{EmailConnector, EmailStatus}
import controllers.PSRController
import _root_.config.FrontendAppConfig
import controllers.actions._
import _root_.config.Constants._
import models.{DateRange, NormalMode, UserAnswers}
import models.requests.DataRequest
import viewmodels.implicits._
import utils.nonsipp.MemberCountUtils.hasMemberNumbersChangedToOver99
import views.html.ContentPageView
import models.SchemeId.Srn
import pages.nonsipp.FbVersionPage
import navigation.Navigator
import utils.nonsipp.SchemeDetailNavigationUtils
import uk.gov.hmrc.http.HeaderCarrier
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.declaration.PsaDeclarationPage
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class PsaDeclarationController @Inject() (
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
  auditService: AuditService,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport
    with SchemeDetailNavigationUtils {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      if (request.pensionSchemeId.isPSP) {
        Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
      } else {
        request.session
          .get(SUBMISSION_VIEWED_FLAG)
          .fold(
            isJourneyBypassed(srn).map(eitherJourneyNavigationResultOrRecovery =>
              eitherJourneyNavigationResultOrRecovery.fold(
                _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()),
                isBypassed =>
                  Ok(
                    view(
                      PsaDeclarationController
                        .viewModel(
                          srn,
                          isBypassed && hasMemberNumbersChangedToOver99(
                            request.userAnswers,
                            srn,
                            request.pensionSchemeId,
                            isPrePopulation
                          )
                        )
                    )
                  )
              )
            )
          )(_ =>
            Future.successful(
              Redirect(controllers.routes.OverviewController.onPageLoad(srn))
                .removingFromSession(SUBMISSION_VIEWED_FLAG)
            )
          )
      }
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val fbVersion = request.userAnswers
        .get(FbVersionPage(srn))
        .getOrElse(defaultFbVersion) // 000 as no versions yet - initial submission
      schemeDateService.schemeDate(srn) match {
        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(dates) =>
          val now = schemeDateService.now()

          for {
            journeyByPassed <- isJourneyBypassed(srn)
            bypassed = journeyByPassed.getOrElse(false)
            _ <-
              if (
                bypassed && hasMemberNumbersChangedToOver99(
                  request.userAnswers,
                  srn,
                  request.pensionSchemeId,
                  isPrePopulation
                )
              ) {
                psrSubmissionService.submitPsrDetailsBypassed(
                  srn = srn,
                  fallbackCall = controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn)
                )
              } else {
                psrSubmissionService.submitPsrDetails(
                  srn = srn,
                  isSubmitted = true,
                  fallbackCall = controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn)
                )
              }
            _ <- sendEmail(
              loggedInUserNameOrBlank,
              request.minimalDetails.email,
              dates,
              request.schemeDetails.schemeName,
              now,
              fbVersion
            )
            _ <- saveService.save(UserAnswers(request.userAnswers.id))
          } yield Redirect(navigator.nextPage(PsaDeclarationPage(srn), NormalMode, request.userAnswers))
            .addingToSession((RETURN_PERIODS, schemeDateService.returnPeriodsAsJsonString(srn)))
            .addingToSession((SUBMISSION_DATE, schemeDateService.submissionDateAsString(now)))
      }
    }

  private def sendEmail(
    name: String,
    email: String,
    taxYear: DateRange,
    schemeName: String,
    submittedDate: LocalDateTime,
    reportVersion: String
  )(implicit
    request: DataRequest[_],
    hc: HeaderCarrier
  ): Future[EmailStatus] = {

    val requestId = hc.requestId.map(_.value).getOrElse(request.headers.get("X-Session-ID").getOrElse(""))

    val templateParams = Map(
      "schemeName" -> schemeName,
      "periodOfReturn" -> taxYear.toSentenceFormat,
      "dateSubmitted" -> DateTimeUtils.formatReadable(submittedDate),
      "psaName" -> name
    )

    emailConnector
      .sendEmail(
        PSA,
        requestId,
        psaOrPspId = request.pensionSchemeId.value,
        request.schemeDetails.pstr,
        email,
        config.fileReturnTemplateId,
        templateParams,
        reportVersion,
        schemeName = request.schemeDetails.schemeName,
        taxYear = taxYear.toYearFormat,
        userName = name
      )
      .map { emailStatus =>
        auditService.sendEvent(
          PSRSubmissionEmailAuditEvent(
            schemeName = request.schemeDetails.schemeName,
            name,
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

  def viewModel(srn: Srn, hasNumberOfMembersChangedToOver99: Boolean = false): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("psaDeclaration.title"),
      Message("psaDeclaration.heading"),
      ContentPageViewModel(),
      routes.PsaDeclarationController.onSubmit(srn),
      optNotificationBanner = if (hasNumberOfMembersChangedToOver99) {
        Some(
          (
            "psaDeclaration.notification.title",
            "psaDeclaration.notification.header",
            "psaDeclaration.notification.paragraph"
          )
        )
      } else {
        None
      }
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
