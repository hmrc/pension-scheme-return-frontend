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

package controllers.nonsipp.memberdetails.upload

import services.{PsrSubmissionService, SaveService, UploadService}
import pages.nonsipp.memberdetails._
import viewmodels.implicits._
import play.api.mvc._
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import navigation.Navigator
import models._
import play.api.i18n._
import config.RefinedTypes.{Max300, OneTo300}
import controllers.PSRController
import views.html.ContentPageView
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import utils.nonsipp.SoftDelete
import pages.nonsipp.memberdetails.upload.FileUploadSuccessPage
import utils.FunctionKUtils._
import viewmodels.DisplayMessage._
import viewmodels.models._
import controllers.nonsipp.memberdetails.upload.FileUploadSuccessController._
import utils.MapUtils.UserAnswersMapOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.{Inject, Named}

class FileUploadSuccessController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  uploadService: UploadService,
  saveService: SaveService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController
    with SoftDelete {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadStatus(UploadKey.fromRequest(srn)).map {
      case Some(upload: UploadStatus.Success) => Ok(view(viewModel(srn, upload.name, mode)))
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn)).flatMap {
      case Some(UploadSuccess(memberDetails)) if memberDetails.nonEmpty =>
        (
          for {
            indexes <- request.userAnswers.membersDetails(srn).refine[Max300.Refined].getOrRecoverJourneyT
            softDeletedMembers <- indexes
              .foldLeft(Try(request.userAnswers))((ua, index) => ua.flatMap(softDeleteMember(srn, index, _)))
              .mapK[Future]
              .liftF
            updatedUserAnswers <- memberDetailsToUserAnswers(srn, sortAlphabetically(memberDetails), softDeletedMembers)
              .mapK[Future]
              .liftF
            _ <- saveService.save(updatedUserAnswers).liftF
            submissionResult <- psrSubmissionService
              .submitPsrDetailsWithUA(
                srn,
                updatedUserAnswers,
                fallbackCall =
                  controllers.nonsipp.memberdetails.upload.routes.FileUploadSuccessController.onPageLoad(srn, mode)
              )
              .liftF
          } yield submissionResult.fold(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))(
            _ => Redirect(navigator.nextPage(FileUploadSuccessPage(srn), mode, updatedUserAnswers))
          )
        ).merge

      case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def sortAlphabetically(memberDetails: List[UploadMemberDetails]): List[UploadMemberDetails] = {
    val sortedMemberDetails = memberDetails.sortBy(_.nameDOB.firstName)
    sortedMemberDetails.zipWithIndex.map { case (details, index) => details.copy(row = index + 1) }
  }

  private def memberDetailsToUserAnswers(
    srn: Srn,
    memberDetails: List[UploadMemberDetails],
    userAnswers: UserAnswers
  ): Try[UserAnswers] = {

    val insertions = memberDetails.flatMap { details =>
      refineV[OneTo300](details.row).toOption.map { index =>
        List(
          UserAnswers.set(MemberDetailsPage(srn, index), details.nameDOB),
          UserAnswers.set(DoesMemberHaveNinoPage(srn, index), details.ninoOrNoNinoReason.isRight),
          details.ninoOrNoNinoReason.fold(
            UserAnswers.set(NoNINOPage(srn, index), _),
            UserAnswers.set(MemberDetailsNinoPage(srn, index), _)
          ),
          UserAnswers.set(MemberStatus(srn, index), MemberState.New),
          UserAnswers.set(SafeToHardDelete(srn, index)),
          UserAnswers.set(MemberDetailsCompletedPage(srn, index), SectionCompleted)
        )
      }
    }.flatten

    UserAnswers.compose(insertions: _*)(userAnswers)
  }
}

object FileUploadSuccessController {
  def viewModel(srn: Srn, fileName: String, mode: Mode): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      title = "fileUploadSuccess.title",
      heading = "fileUploadSuccess.heading",
      ContentPageViewModel(isLargeHeading = true),
      onSubmit = routes.FileUploadSuccessController.onSubmit(srn, mode)
    ).withButtonText("site.continue")
      .withDescription(ParagraphMessage(Message("fileUploadSuccess.paragraph", fileName)))
}
