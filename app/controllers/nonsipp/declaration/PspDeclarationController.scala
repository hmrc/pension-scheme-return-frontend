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
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import connectors.{EmailConnector, EmailStatus}
import controllers.PSRController
import config.FrontendAppConfig
import config.Constants._
import controllers.actions._
import models.{DateRange, NormalMode, UserAnswers}
import views.html.PsaIdInputView
import models.SchemeId.Srn
import pages.nonsipp.FbVersionPage
import navigation.Navigator
import forms.TextFormProvider
import uk.gov.hmrc.http.HeaderCarrier
import play.api.i18n.MessagesApi
import pages.nonsipp.declaration.PspDeclarationPage
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, TextInputViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class PspDeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  formProvider: TextFormProvider,
  view: PsaIdInputView,
  emailConnector: EmailConnector,
  config: FrontendAppConfig,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      def form: Form[String] = PspDeclarationController.form(formProvider, request.schemeDetails.authorisingPSAID)
      Ok(
        view(
          form.fromUserAnswers(PspDeclarationPage(srn)),
          PspDeclarationController.viewModel(srn)
        )
      )
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val fbVersion = request.userAnswers.get(FbVersionPage(srn)).getOrElse("000") // 000 as no versions yet - initial submission
      schemeDateService.schemeDate(srn) match {
        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(dates) =>
          val now = schemeDateService.now()

          PspDeclarationController
            .form(formProvider, request.schemeDetails.authorisingPSAID)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      PspDeclarationController.viewModel(srn)
                    )
                  )
                ),
              answer => {
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(PspDeclarationPage(srn), answer))
                  _ <- saveService.save(updatedAnswers)
                  _ <- psrSubmissionService.submitPsrDetails(
                    srn = srn,
                    isSubmitted = true,
                    fallbackCall = controllers.nonsipp.declaration.routes.PspDeclarationController.onPageLoad(srn)
                  )
                  _ <- sendEmail(
                    loggedInUserNameOrBlank(request),
                    request.minimalDetails.email,
                    dates,
                    request.schemeDetails.schemeName,
                    now,
                    fbVersion
                  )
                  _ <- saveService.save(UserAnswers(request.userAnswers.id))
                } yield {
                  Redirect(navigator.nextPage(PspDeclarationPage(srn), NormalMode, request.userAnswers))
                    .addingToSession((RETURN_PERIODS, schemeDateService.returnPeriodsAsJsonString(srn)))
                    .addingToSession(
                      (SUBMISSION_DATE, schemeDateService.submissionDateAsString(now))
                    )
                }
              }
            )
      }
    }

  private def sendEmail(
    name: String,
    email: String,
    taxYear: DateRange,
    schemeName: String,
    submittedDate: LocalDateTime,
    reportVersion: String
  )(
    implicit request: DataRequest[_],
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
        PSP,
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
            credentialRole = PSP,
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

object PspDeclarationController {
  def form(formProvider: TextFormProvider, authorisingPsaId: Option[String]): Form[String] = formProvider.psaId(
    "pspDeclaration.psaId.error.required",
    "pspDeclaration.psaId.error.invalid.characters",
    "pspDeclaration.psaId.error.invalid.characters",
    "pspDeclaration.psaId.error.invalid.noMatch",
    authorisingPsaId
  )

  def viewModel(srn: Srn): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("pspDeclaration.title"),
      Message("pspDeclaration.heading"),
      TextInputViewModel(Some(Message("pspDeclaration.psaId.label")), isFixedLength = true),
      routes.PspDeclarationController.onSubmit(srn)
    ).withButtonText(Message("site.agreeAndContinue"))
      .withDescription(
        ParagraphMessage("pspDeclaration.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "pspDeclaration.listItem1",
            "pspDeclaration.listItem2",
            "pspDeclaration.listItem3",
            "pspDeclaration.listItem4",
            "pspDeclaration.listItem5"
          )
      )

}
