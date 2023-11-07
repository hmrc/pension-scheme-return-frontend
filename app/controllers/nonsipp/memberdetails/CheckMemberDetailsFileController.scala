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

package controllers.nonsipp.memberdetails

import akka.stream.Materializer
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.memberdetails.CheckMemberDetailsFileController._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.UploadStatus.UploadStatus
import models.audit.{PSRUpscanFileDownloadAuditEvent, PSRUpscanFileUploadAuditEvent}
import models.requests.DataRequest
import models.{DateRange, Mode, UploadKey, UploadStatus}
import navigation.Navigator
import pages.nonsipp.memberdetails.CheckMemberDetailsFilePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, MemberDetailsUploadValidator, SaveService, SchemeDateService, UploadService}
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CheckMemberDetailsFileController @Inject()(
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
    extends PSRController
    with I18nSupport {

  private val form = CheckMemberDetailsFileController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val startTime = System.currentTimeMillis
    val preparedForm = request.userAnswers.fillForm(CheckMemberDetailsFilePage(srn), form)
    val uploadKey = UploadKey.fromRequest(srn)

    uploadService.getUploadStatus(uploadKey).map {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(upload: UploadStatus.Success) => {
        auditUpload(srn, upload, startTime)
        Ok(view(preparedForm, viewModel(srn, Some(upload.name), mode)))
      }
      case Some(failure: UploadStatus.Failed) =>
        auditUpload(srn, failure, startTime)
        Ok(view(preparedForm, viewModel(srn, Some(""), mode)))
      case Some(_) => Ok(view(preparedForm, viewModel(srn, None, mode)))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val startTime = System.currentTimeMillis
    val uploadKey = UploadKey.fromRequest(srn)

    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getUploadedFile(uploadKey)
            .map(file => BadRequest(view(formWithErrors, viewModel(srn, file.map(_.name), mode)))),
        value =>
          getUploadedFile(uploadKey).flatMap {
            case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            case Some(file) =>
              for {
                source <- {
                  uploadService.stream(file.downloadUrl)
                }
                validated <- {
                  auditDownload(srn, source._1, startTime)
                  uploadValidator.validateCSV(source._2, srn, request)
                }
                _ <- uploadService.saveValidatedUpload(uploadKey, validated._1)
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckMemberDetailsFilePage(srn), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(CheckMemberDetailsFilePage(srn), mode, updatedAnswers))
          }
      )
  }

  // todo: handle all Upscan upload states
  //       None is an error case as the initial state set on the previous page should be InProgress
  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }

  private def buildUploadAuditEvent(taxYear: DateRange, uploadStatus: UploadStatus, duration: Long)(
    implicit req: DataRequest[_]
  ) = PSRUpscanFileUploadAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    schemeAdministratorName = req.schemeDetails.establishers.headOption.get.name,
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) "PSP" else "PSA",
    taxYear = taxYear,
    uploadStatus,
    duration
  )

  private def auditUpload(srn: Srn, uploadStatus: UploadStatus, startTime: Long)(
    implicit request: DataRequest[_]
  ): Unit = {
    val endTime = System.currentTimeMillis
    val duration = endTime - startTime
    for {
      taxYear <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney
      _ = auditService.sendEvent(buildUploadAuditEvent(taxYear, uploadStatus, duration))
    } yield taxYear
  }

  private def buildDownloadAuditEvent(taxYear: DateRange, responseStatus: Int, duration: Long)(
    implicit req: DataRequest[_]
  ) = PSRUpscanFileDownloadAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    schemeAdministratorName = req.schemeDetails.establishers.headOption.get.name,
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) "PSP" else "PSA",
    taxYear = taxYear,
    downloadStatus = responseStatus match {
      case 200 => "Success"
      case _ => "Failed"
    },
    duration
  )

  private def auditDownload(srn: Srn, responseStatus: Int, startTime: Long)(implicit request: DataRequest[_]): Unit = {
    val endTime = System.currentTimeMillis
    val duration = endTime - startTime
    for {
      taxYear <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney
      _ = auditService.sendEvent(buildDownloadAuditEvent(taxYear, responseStatus, duration))
    } yield taxYear
  }
}

object CheckMemberDetailsFileController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "checkMemberDetailsFile.error.required"
  )

  def viewModel(srn: Srn, fileName: Option[String], mode: Mode): FormPageViewModel[YesNoPageViewModel] = {
    val refresh = if (fileName.isEmpty) Some(1) else None
    FormPageViewModel(
      "checkMemberDetailsFile.title",
      "checkMemberDetailsFile.heading",
      YesNoPageViewModel(
        legend = Some("checkMemberDetailsFile.legend"),
        yes = Some("checkMemberDetailsFile.yes"),
        no = Some("checkMemberDetailsFile.no")
      ),
      onSubmit = routes.CheckMemberDetailsFileController.onSubmit(srn, mode)
    ).refreshPage(refresh)
      .withDescription(
        fileName.map(name => ParagraphMessage(name))
      )
  }
}
