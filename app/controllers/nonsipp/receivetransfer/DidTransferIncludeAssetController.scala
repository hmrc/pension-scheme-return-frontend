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

package controllers.nonsipp.receivetransfer

import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import controllers.PSRController
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import services.SaveService
import DidTransferIncludeAssetController._
import viewmodels.implicits._
import pages.nonsipp.receivetransfer.{DidTransferIncludeAssetPage, TransferringSchemeNamePage}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import config.Refined._
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.DisplayMessage.Message

class DidTransferIncludeAssetController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = DidTransferIncludeAssetController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(DidTransferIncludeAssetPage(srn, index, secondaryIndex)).fold(form)(form.fill)
      (
        for {
          schemeName <- request.userAnswers
            .get(TransferringSchemeNamePage(srn, index, secondaryIndex))
            .getOrRecoverJourney
          memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
        } yield Ok(
          view(
            preparedForm,
            viewModel(srn, schemeName, memberDetails.fullName, index, secondaryIndex, mode)
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              (for {
                schemeName <- request.userAnswers
                  .get(TransferringSchemeNamePage(srn, index, secondaryIndex))
                  .getOrRecoverJourney
                memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
              } yield BadRequest(
                view(
                  formWithErrors,
                  viewModel(
                    srn,
                    schemeName,
                    memberDetails.fullName,
                    index,
                    secondaryIndex,
                    mode
                  )
                )
              )).merge
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(DidTransferIncludeAssetPage(srn, index, secondaryIndex), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(DidTransferIncludeAssetPage(srn, index, secondaryIndex), mode, updatedAnswers)
            )
        )
    }
}

object DidTransferIncludeAssetController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "didTransferIncludeAsset.error.required"
  )

  def viewModel(
    srn: Srn,
    schemeName: String,
    memberName: String,
    index: Max300,
    secondaryIndex: Max5,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "didTransferIncludeAsset.title",
      Message("didTransferIncludeAsset.heading", schemeName, memberName),
      controllers.nonsipp.receivetransfer.routes.DidTransferIncludeAssetController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
