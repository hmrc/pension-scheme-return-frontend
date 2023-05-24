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

import config.FrontendAppConfig
import controllers.actions._
import controllers.nonsipp.memberdetails.UploadMemberDetailsController._
import models.SchemeId.Srn
import models.{Mode, Reference, UploadKey}
import navigation.Navigator
import pages.nonsipp.memberdetails.UploadMemberDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, UploadViewModel}
import views.html.UploadView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class UploadMemberDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  view: UploadView,
  uploadService: UploadService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def callBackUrl(implicit req: Request[_]): String =
    controllers.routes.UploadCallbackController.callback.absoluteURL(secure = config.secureUpscanCallBack)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val successRedirectUrl =
      controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onSubmit(srn).absoluteURL()
    val failureRedirectUrl =
      controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn).absoluteURL()

    val uploadKey = UploadKey.fromRequest(srn)

    for {
      initiateResponse <- uploadService.initiateUpscan(callBackUrl, successRedirectUrl, failureRedirectUrl)
      _ <- uploadService.registerUploadRequest(uploadKey, Reference(initiateResponse.fileReference.reference))
    } yield Ok(view(viewModel(initiateResponse.postTarget, initiateResponse.formFields, collectErrors)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(UploadMemberDetailsPage(srn), mode, request.userAnswers))
  }

  private def collectErrors(implicit request: Request[_]): Option[String] =
    request.getQueryString("errorCode").zip(request.getQueryString("errorMessage")).flatMap {
      case ("EntityTooLarge", _) => Some("uploadMemberDetails.error.size")
      case ("InvalidArgument", "'file' field not found") => Some("uploadMemberDetails.error.required")
      case _ => None
    }
}

object UploadMemberDetailsController {

  def viewModel(
    postTarget: String,
    formFields: Map[String, String],
    error: Option[String]
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
        maxFileSize = "100MB",
        formFields,
        error
      ),
      Call("POST", postTarget)
    ).withDescription(ParagraphMessage("uploadMemberDetails.paragraph"))
}
