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
import controllers.actions._
import controllers.nonsipp.memberdetails.CheckMemberDetailsFileController._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Mode, UploadKey, UploadStatus}
import navigation.Navigator
import pages.nonsipp.memberdetails.CheckMemberDetailsFilePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{MemberDetailsUploadValidator, SaveService, UploadService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits._
import viewmodels.models.{PageViewModel, YesNoPageViewModel}
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
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext, mat: Materializer)
    extends FrontendBaseController
    with I18nSupport {

  private val form = CheckMemberDetailsFileController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val preparedForm = request.userAnswers.fillForm(CheckMemberDetailsFilePage(srn), form)
    val uploadKey = UploadKey.fromRequest(srn)

    uploadService.getUploadResult(uploadKey).map {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(upload: UploadStatus.Success) =>
        Ok(view(preparedForm, viewModel(srn, Some(upload.name), mode)))
      case Some(_) => Ok(view(preparedForm, viewModel(srn, None, mode)))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
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
                source <- uploadService.stream(file.downloadUrl)
                validated <- uploadValidator.validateCSV(source)
                _ <- uploadService.saveValidatedUpload(uploadKey, validated)
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
      .getUploadResult(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }
}

object CheckMemberDetailsFileController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "checkMemberDetailsFile.error.required"
  )

  def viewModel(srn: Srn, fileName: Option[String], mode: Mode): PageViewModel[YesNoPageViewModel] = {
    val refresh = if (fileName.isEmpty) Some(1) else None
    PageViewModel(
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
