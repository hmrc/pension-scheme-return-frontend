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

package controllers.nonsipp.memberdetails

import services._
import models.audit.{PSRFileValidationAuditEvent, PSRUpscanFileDownloadAuditEvent, PSRUpscanFileUploadAuditEvent}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import org.apache.pekko.stream.Materializer
import controllers.PSRController
import config.Constants.{PSA, PSP}
import controllers.actions._
import models._
import pages.nonsipp.memberdetails.CheckMemberDetailsFilePage
import controllers.nonsipp.memberdetails.CheckMemberDetailsFileController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.Logger
import navigation.Navigator
import forms.YesNoPageFormProvider
import pages.nonsipp.memberdetails.upload.UploadStatusPage
import models.UploadStatus.UploadStatus
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CheckMemberDetailsFileController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  uploadService: UploadService,
  uploadValidator: MemberDetailsUploadValidator,
  schemeDateService: SchemeDateService,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext, mat: Materializer)
    extends PSRController {

  private val logger = Logger(getClass)
  private val form = CheckMemberDetailsFileController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val uploadStatusOpt = request.userAnswers.get(UploadStatusPage(srn))

    uploadStatusOpt match {
      case Some(UploadSubmitted) =>
        val startTime = System.currentTimeMillis
        val uploadKey = UploadKey.fromRequest(srn)
        val preparedForm = request.userAnswers.fillForm(CheckMemberDetailsFilePage(srn), form)

        uploadService.getUploadStatus(uploadKey).map {
          case None =>
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

          case Some(uploadStatus) =>
            uploadStatus match {
              case success: UploadStatus.Success =>
                auditUpload(srn, success, startTime)
                Ok(view(preparedForm, viewModel(srn, Some(success.name), mode)))

              case failed: UploadStatus.Failed =>
                auditUpload(srn, failed, startTime)
                Redirect(routes.UploadMemberDetailsController.onPageLoad(srn, true))

              case UploadStatus.InProgress =>
                Ok(view(preparedForm, viewModel(srn, None, mode)))
            }
        }

      case _ =>
        logger.warn("Upload is either initiated or never started, redirecting to upload member details")
        Future.successful(Redirect(routes.UploadMemberDetailsController.onPageLoad(srn, false)))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val startTime = System.currentTimeMillis
    val uploadKey = UploadKey.fromRequest(srn)

    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getUploadedFile(uploadKey).map {
            case Some(uploadStatus) =>
              val fileName = uploadStatus match {
                case success: UploadStatus.Success => Some(success.name)
                case _ => None
              }
              BadRequest(view(formWithErrors, viewModel(srn, fileName, mode)))
            case None =>
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          },
        value =>
          getUploadedFile(uploadKey).flatMap {
            case Some(success: UploadStatus.Success) =>
              for {
                source <- uploadService.stream(success.downloadUrl)
                validated <- {
                  auditDownload(srn, source._1, startTime)
                  uploadValidator.validateCSV(source._2, None)
                }
                _ <- {
                  auditValidation(srn, validated)
                  uploadService.saveValidatedUpload(uploadKey, validated._1)
                }
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckMemberDetailsFilePage(srn), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(CheckMemberDetailsFilePage(srn), mode, updatedAnswers))

            case Some(failed: UploadStatus.Failed) =>
              auditUpload(srn, failed, startTime)
              Future.successful(
                Redirect(routes.UploadMemberDetailsController.onPageLoad(srn, true))
              )

            case Some(UploadStatus.InProgress) =>
              Future.successful(Redirect(routes.CheckMemberDetailsFileController.onPageLoad(srn, mode)))

            case None =>
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      )
  }
  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus]] =
    uploadService
      .getUploadStatus(uploadKey)

  private def buildUploadAuditEvent(taxYear: DateRange, uploadStatus: UploadStatus, duration: Long, userName: String)(
    implicit req: DataRequest[?]
  ) = PSRUpscanFileUploadAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    schemeAdministratorOrPractitionerName = req.schemeDetails.establishers.headOption.fold(userName)(e => e.name),
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) PSP else PSA,
    taxYear = taxYear,
    uploadStatus,
    duration
  )

  private def auditUpload(srn: Srn, uploadStatus: UploadStatus, startTime: Long)(implicit
    request: DataRequest[?]
  ): Unit = {
    val endTime = System.currentTimeMillis
    val duration = endTime - startTime
    schemeDateService
      .taxYearOrAccountingPeriods(srn)
      .merge
      .getOrRecoverJourney
      .flatMap(taxYear =>
        loggedInUserNameOrRedirect
          .map(userName => auditService.sendEvent(buildUploadAuditEvent(taxYear, uploadStatus, duration, userName)))
      )
  }

  private def buildDownloadAuditEvent(taxYear: DateRange, responseStatus: Int, duration: Long, userName: String)(
    implicit req: DataRequest[?]
  ) = PSRUpscanFileDownloadAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    schemeAdministratorOrPractitionerName = req.schemeDetails.establishers.headOption.fold(userName)(e => e.name),
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) PSP else PSA,
    taxYear = taxYear,
    downloadStatus = responseStatus match {
      case 200 => "Success"
      case _ => "Failed"
    },
    duration
  )

  private def auditDownload(srn: Srn, responseStatus: Int, duration: Long)(implicit
    request: DataRequest[?]
  ): Unit =
    schemeDateService
      .taxYearOrAccountingPeriods(srn)
      .merge
      .getOrRecoverJourney
      .flatMap(taxYear =>
        loggedInUserNameOrRedirect.map(userName =>
          auditService.sendEvent(buildDownloadAuditEvent(taxYear, responseStatus, duration, userName))
        )
      )

  private def auditValidation(srn: Srn, outcome: (Upload, Int, Long))(implicit
    request: DataRequest[?]
  ): Unit =
    schemeDateService
      .taxYearOrAccountingPeriods(srn)
      .merge
      .getOrRecoverJourney
      .flatMap(taxYear =>
        loggedInUserNameOrRedirect
          .map(userName => auditService.sendEvent(buildValidationAuditEvent(taxYear, outcome, userName)))
      )

  private def buildValidationAuditEvent(taxYear: DateRange, outcome: (Upload, Int, Long), userName: String)(implicit
    req: DataRequest[?]
  ) = PSRFileValidationAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    schemeAdministratorOrPractitionerName = req.schemeDetails.establishers.headOption.fold(userName)(e => e.name),
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) PSP else PSA,
    taxYear = taxYear,
    validationCheckStatus = outcome._1 match {
      case _: UploadSuccess => "Success"
      case _ => "Failed"
    },
    fileValidationTimeInMilliSeconds = outcome._3,
    numberOfEntries = outcome._2,
    numberOfFailures = outcome._1 match {
      case _: UploadSuccess => 0
      case errors: UploadErrors => errors.errors.size
      case _: UploadMaxRowsError.type => 1
      case _: UploadFormatError.type => 1
      case null => 0
    }
  )
}

object CheckMemberDetailsFileController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "checkMemberDetailsFile.error.required"
  )

  def viewModel(
    srn: Srn,
    fileName: Option[String],
    mode: Mode,
    doNotRefresh: Boolean = true
  ): FormPageViewModel[YesNoPageViewModel] = {
    val refresh = if (fileName.isEmpty && doNotRefresh) Some(1) else None
    FormPageViewModel(
      "checkMemberDetailsFile.title",
      "checkMemberDetailsFile.heading",
      YesNoPageViewModel(
        legend = Some("checkMemberDetailsFile.legend"),
        hint = None,
        yes = Some("checkMemberDetailsFile.yes"),
        no = Some("checkMemberDetailsFile.no"),
        showRadios = doNotRefresh
      ),
      onSubmit =
        if (doNotRefresh) routes.CheckMemberDetailsFileController.onSubmit(srn, mode)
        else routes.UploadMemberDetailsController.onPageLoad(srn, false)
    ).refreshPage(refresh)
      .withDescription(
        fileName.map(name => ParagraphMessage(name))
      )
  }

}
