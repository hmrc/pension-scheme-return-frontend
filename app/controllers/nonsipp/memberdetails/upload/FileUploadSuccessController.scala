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

package controllers.nonsipp.memberdetails.upload

import config.Refined.OneTo300
import controllers.actions._
import controllers.nonsipp.memberdetails.upload.FileUploadSuccessController._
import eu.timepit.refined.{refineMV, refineV}
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, UploadKey, UploadMemberDetails, UploadStatus, UploadSuccess, UserAnswers}
import navigation.Navigator
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberdetails.upload.FileUploadSuccessPage
import play.api.i18n._
import play.api.mvc._
import services.{SaveService, UploadService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class FileUploadSuccessController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  uploadService: UploadService,
  saveService: SaveService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadStatus(UploadKey.fromRequest(srn)).map {
      case Some(upload: UploadStatus.Success) => Ok(view(viewModel(srn, upload.name, mode)))
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn)).flatMap {
      case Some(UploadSuccess(memberDetails)) if memberDetails.nonEmpty =>
        for {
          updatedUserAnswers <- Future.fromTry(memberDetailsToUserAnswers(srn, sortAlphabetically(memberDetails)))
          _ <- saveService.save(updatedUserAnswers)
        } yield Redirect(navigator.nextPage(FileUploadSuccessPage(srn), mode, updatedUserAnswers))
      case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def sortAlphabetically(memberDetails: List[UploadMemberDetails]): List[UploadMemberDetails] = {
    val sortedMemberDetails = memberDetails.sortBy(_.nameDOB.firstName)
    sortedMemberDetails.zipWithIndex.map { case (details, index) => details.copy(row = index + 1) }
  }

  private def memberDetailsToUserAnswers(srn: Srn, memberDetails: List[UploadMemberDetails])(
    implicit request: DataRequest[_]
  ): Try[UserAnswers] = {

    val removals = List(
      UserAnswers.remove(MembersDetailsPages(srn)),
      UserAnswers.remove(DoesMemberHaveNinoPage(srn, refineMV(1))),
      UserAnswers.remove(MemberDetailsNinoPages(srn)),
      UserAnswers.remove(NoNinoPages(srn))
    )

    val insertions = memberDetails.flatMap { details =>
      refineV[OneTo300](details.row).toOption.map { index =>
        List(
          UserAnswers.set(MemberDetailsPage(srn, index), details.nameDOB)
//          UserAnswers.set(DoesMemberHaveNinoPage(srn, index), details.ninoOrNoNinoReason.isRight)
//          details.ninoOrNoNinoReason.fold(
//            UserAnswers.set(NoNINOPage(srn, index), _),
//            UserAnswers.set(MemberDetailsNinoPage(srn, index), _)
//          )
        )
      }
    }.flatten

    UserAnswers.compose(removals ++ insertions: _*)(request.userAnswers)
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
