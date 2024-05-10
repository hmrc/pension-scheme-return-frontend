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

package controllers.nonsipp.memberreceivedpcls

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.Max300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Money, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, RemovePclsPage}
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemovePclsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val form = RemovePclsController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val nameDOB = request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).get
      val total = request.userAnswers.get(PensionCommencementLumpSumAmountPage(srn, memberIndex))
      total match {
        case Some(value) =>
          Ok(
            view(
              form,
              RemovePclsController.viewModel(
                srn,
                memberIndex: Max300,
                value.lumpSumAmount,
                nameDOB.fullName
              )
            )
          )
        case None =>
          Redirect(
            controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumController
              .onPageLoad(srn, NormalMode)
              .url
          )
      }
    }

  def onSubmit(srn: Srn, memberIndex: Max300): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                total <- request.userAnswers
                  .get(PensionCommencementLumpSumAmountPage(srn, memberIndex))
                  .getOrRecoverJourneyT
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourneyT
              } yield BadRequest(
                view(
                  formWithErrors,
                  RemovePclsController.viewModel(srn, memberIndex, total.lumpSumAmount, nameDOB.fullName)
                )
              )
            ).merge
          },
          removeDetails => {
            if (removeDetails) {
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.remove(PensionCommencementLumpSumAmountPage(srn, memberIndex))
                )
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall = controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
                    .onPageLoad(srn, 1, NormalMode)
                )
              } yield submissionResult.fold(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(RemovePclsPage(srn, memberIndex), NormalMode, updatedAnswers)
                  )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemovePclsPage(srn, memberIndex), NormalMode, request.userAnswers)
                  )
                )
            }
          }
        )
    }
}

object RemovePclsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "pcls.removePcls.error.required"
  )

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    total: Money,
    fullName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("pcls.removePcls.title"),
      Message("pcls.removePcls.heading", total.displayAs, fullName),
      routes.RemovePclsController.onSubmit(srn, memberIndex)
    )
}
