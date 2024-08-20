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
import models.audit.PSRUpscanFileUploadAuditEvent
import pages.nonsipp.memberdetails.UploadMemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import controllers.PSRController
import config.FrontendAppConfig
import controllers.nonsipp.memberdetails.UploadMemberDetailsController._
import config.Constants.{PSA, PSP}
import controllers.actions._
import navigation.Navigator
import pages.nonsipp.memberdetails.upload.UploadStatusPage
import models._
import views.html.UploadView
import models.SchemeId.Srn
import models.UploadStatus.UploadStatus
import play.api.i18n.{I18nSupport, MessagesApi}
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{ListMessage, ListType, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, UploadViewModel}
import models.requests.DataRequest
import play.api.data.FormError

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class UploadMemberDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  view: UploadView,
  uploadService: UploadService,
  schemeDateService: SchemeDateService,
  auditService: AuditService,
  config: FrontendAppConfig,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def callBackUrl(implicit req: Request[_]): String =
    controllers.routes.UploadCallbackController.callback.absoluteURL(secure = config.secureUpscanCallBack)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val redirectTag = "upload-member-details"
    val successRedirectUrl = config.urls.upscan.successEndpoint.format(srn.value, redirectTag)
    val failureRedirectUrl = config.urls.upscan.failureEndpoint.format(srn.value, redirectTag)
    val startTime = System.currentTimeMillis

    val uploadKey = UploadKey.fromRequest(srn)

    for {
      initiateResponse <- uploadService.initiateUpscan(callBackUrl, successRedirectUrl, failureRedirectUrl)
      _ <- uploadService.registerUploadRequest(uploadKey, Reference(initiateResponse.fileReference.reference))
      updatedUserAnswers <- request.userAnswers.set(UploadStatusPage(srn), UploadInitiated).mapK[Future]
      _ <- saveService.save(updatedUserAnswers)
    } yield Ok(
      view(
        viewModel(
          initiateResponse.postTarget,
          initiateResponse.formFields,
          collectErrors(srn, startTime),
          config.upscanMaxFileSizeMB
        )
      )
    )
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    for {
      updatedUserAnswers <- request.userAnswers.set(UploadStatusPage(srn), UploadSubmitted).mapK[Future]
      _ <- saveService.save(updatedUserAnswers)
    } yield Redirect(navigator.nextPage(UploadMemberDetailsPage(srn), mode, request.userAnswers))
  }

  private def collectErrors(srn: Srn, startTime: Long)(implicit request: DataRequest[_]): Option[FormError] =
    request.getQueryString("errorCode").zip(request.getQueryString("errorMessage")).flatMap {
      case ("EntityTooLarge", error) =>
        val failure = UploadStatus.Failed(ErrorDetails("EntityTooLarge", error))
        audit(srn, failure, startTime)
        Some(FormError("file-input", "uploadMemberDetails.error.size", Seq(config.upscanMaxFileSizeMB)))
      case ("InvalidArgument", "'file' field not found") =>
        val failure = UploadStatus.Failed(ErrorDetails("InvalidArgument", "'file' field not found"))
        audit(srn, failure, startTime)
        Some(FormError("file-input", "uploadMemberDetails.error.required"))
      case _ => None
    }

  private def buildAuditEvent(taxYear: DateRange, uploadStatus: UploadStatus, duration: Long, userName: String)(
    implicit req: DataRequest[_]
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

  private def audit(srn: Srn, uploadStatus: UploadStatus, startTime: Long)(implicit request: DataRequest[_]): Unit = {
    val endTime = System.currentTimeMillis
    val duration = endTime - startTime
    for {
      taxYear <- {
        val x = schemeDateService.taxYearOrAccountingPeriods(srn)
        x.merge.getOrRecoverJourney
      }
      userName <- loggedInUserNameOrRedirect
      _ = auditService.sendEvent(buildAuditEvent(taxYear, uploadStatus, duration, userName))
    } yield taxYear
  }
}

object UploadMemberDetailsController {

  def viewModel(
    postTarget: String,
    formFields: Map[String, String],
    error: Option[FormError],
    maxFileSize: String
  ): FormPageViewModel[UploadViewModel] =
    FormPageViewModel(
      "uploadMemberDetails.title",
      "uploadMemberDetails.heading",
      UploadViewModel(
        detailsContent =
          ParagraphMessage("uploadMemberDetails.details.paragraph") ++
            ListMessage(
              ListType.Bullet,
              "uploadMemberDetails.list1",
              "uploadMemberDetails.list2",
              "uploadMemberDetails.list3"
            ),
        acceptedFileType = ".csv",
        maxFileSize = maxFileSize,
        formFields,
        error
      ),
      Call("POST", postTarget)
    ).withDescription(ParagraphMessage("uploadMemberDetails.paragraph"))
}
