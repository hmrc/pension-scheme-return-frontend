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

package controllers.nonsipp.membertransferout

import config.Refined.{Max300, Max5}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.membertransferout.{transferOutPages, ReceivingSchemeNamePage, RemoveTransferOutPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveTransferOutController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val form = RemoveTransferOutController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, index: Max5): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val nameDOB = request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).get
      val receivingSchemeName = request.userAnswers.get(ReceivingSchemeNamePage(srn, memberIndex, index))
      receivingSchemeName match {
        case Some(schemeName) =>
          Ok(
            view(
              form,
              RemoveTransferOutController
                .viewModel(srn, memberIndex: Max300, index: Max5, nameDOB.fullName, schemeName)
            )
          )
        case None =>
          Redirect(
            controllers.nonsipp.membertransferout.routes.SchemeTransferOutController.onPageLoad(srn, NormalMode).url
          )
      }
    }

  def onSubmit(srn: Srn, memberIndex: Max300, index: Max5): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourneyT
                receivingSchemeName <- request.userAnswers
                  .get(ReceivingSchemeNamePage(srn, memberIndex, index))
                  .getOrRecoverJourneyT
              } yield BadRequest(
                view(
                  formWithErrors,
                  RemoveTransferOutController.viewModel(srn, memberIndex, index, nameDOB.fullName, receivingSchemeName)
                )
              )
            ).merge
          },
          removeDetails => {
            if (removeDetails) {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.remove(transferOutPages(srn, memberIndex, index)))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator
                  .nextPage(RemoveTransferOutPage(srn, memberIndex), NormalMode, updatedAnswers)
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemoveTransferOutPage(srn, memberIndex), NormalMode, request.userAnswers)
                  )
                )
            }
          }
        )
    }
}

object RemoveTransferOutController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "transferOut.removeTransferOut.error.required"
  )

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    index: Max5,
    fullName: String,
    receivingSchemeName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("transferOut.removeTransferOut.title"),
      Message("transferOut.removeTransferOut.heading", receivingSchemeName, fullName),
      routes.RemoveTransferOutController.onSubmit(srn, memberIndex, index)
    )
}
