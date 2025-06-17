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

package controllers.nonsipp.membersurrenderedbenefits

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined300}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import pages.nonsipp.membersurrenderedbenefits._
import models.{Money, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveSurrenderedBenefitsController @Inject() (
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

  private val form = RemoveSurrenderedBenefitsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRedirectToTaskList(srn)
          surrenderedBenefitsAmount <- request.userAnswers
            .get(SurrenderedBenefitsAmountPage(srn, index))
            .getOrRedirectToTaskList(srn)
        } yield Ok(
          view(
            form,
            RemoveSurrenderedBenefitsController.viewModel(
              srn,
              index: Max300,
              surrenderedBenefitsAmount,
              nameDOB.fullName
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            (
              for {
                total <- request.userAnswers
                  .get(SurrenderedBenefitsAmountPage(srn, index))
                  .getOrRecoverJourneyT
                nameDOB <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourneyT
              } yield BadRequest(
                view(
                  formWithErrors,
                  RemoveSurrenderedBenefitsController.viewModel(srn, index, total, nameDOB.fullName)
                )
              )
            ).merge,
          removeDetails =>
            if (removeDetails) {
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .removeOnlyMultiplePages(
                        surrenderBenefitsPages(srn, index)
                      )
                      .set(MemberStatus(srn, index), MemberState.Changed)
                  )
                _ <- saveService.save(updatedAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall =
                    controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
                      .onPageLoad(srn, 1, NormalMode)
                )
              } yield submissionResult.fold(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))(_ =>
                Redirect(
                  navigator
                    .nextPage(RemoveSurrenderedBenefitsPage(srn, index), NormalMode, updatedAnswers)
                )
              )
            } else {
              Future
                .successful(
                  Redirect(
                    navigator
                      .nextPage(RemoveSurrenderedBenefitsPage(srn, index), NormalMode, request.userAnswers)
                  )
                )
            }
        )
    }
}

object RemoveSurrenderedBenefitsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeSurrenderedBenefits.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    total: Money,
    fullName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removeSurrenderedBenefits.title"),
      Message("removeSurrenderedBenefits.heading", total.displayAs, fullName),
      routes.RemoveSurrenderedBenefitsController.onSubmit(srn, index)
    )
}
