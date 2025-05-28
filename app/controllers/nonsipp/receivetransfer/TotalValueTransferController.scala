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

package controllers.nonsipp.receivetransfer

import services.SaveService
import viewmodels.implicits._
import controllers.nonsipp.receivetransfer.TotalValueTransferController._
import play.api.mvc._
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import utils.IntUtils.{toInt, IntOpts}
import pages.nonsipp.receivetransfer.{TotalValueTransferPage, TransferringSchemeNamePage}
import cats.syntax.applicative._
import controllers.actions._
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.RefinedTypes.{Max300, Max5}
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import play.api.Logger
import navigation.Navigator
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalValueTransferController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private implicit val logger: Logger = Logger(getClass)
  private val form = TotalValueTransferController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, secondaryIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          completedMemberDetails <- request.userAnswers.completedMemberDetails(srn, index.refined).getOrRecoverJourney
          (_, memberDetails) = completedMemberDetails
          transferSchemeName <- request.userAnswers
            .get(TransferringSchemeNamePage(srn, index.refined, secondaryIndex.refined))
            .getOrRecoverJourney
          preparedForm = request.userAnswers
            .get(TotalValueTransferPage(srn, index.refined, secondaryIndex.refined))
            .fold(form)(form.fill)
        } yield Ok(
          view(
            preparedForm,
            viewModel(
              srn,
              index.refined,
              secondaryIndex.refined,
              memberDetails.fullName,
              transferSchemeName,
              form,
              mode
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Int, secondaryIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            (
              for {
                completedMemberDetails <- request.userAnswers
                  .completedMemberDetails(srn, index.refined)
                  .getOrRecoverJourney
                (_, memberDetails) = completedMemberDetails
                transferSchemeName <- request.userAnswers
                  .get(TransferringSchemeNamePage(srn, index.refined, secondaryIndex.refined))
                  .getOrRecoverJourney
              } yield BadRequest(
                view(
                  formWithErrors,
                  viewModel(
                    srn,
                    index.refined,
                    secondaryIndex.refined,
                    memberDetails.fullName,
                    transferSchemeName,
                    form,
                    mode
                  )
                )
              )
            ).merge.pure[Future],
          value =>
            for {
              updatedAnswers <- request.userAnswers
                .set(TotalValueTransferPage(srn, index.refined, secondaryIndex.refined), value)
                .mapK[Future]
              nextPage = navigator
                .nextPage(TotalValueTransferPage(srn, index.refined, secondaryIndex.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(
                srn,
                index.refined,
                secondaryIndex.refined,
                updatedAnswers,
                nextPage
              )
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object TotalValueTransferController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalValueTransfer.error.required",
      "totalValueTransfer.error.invalid",
      (Constants.maxMoneyValue, "totalValueTransfer.error.tooLarge"),
      (Constants.minPosMoneyValue, "totalValueTransfer.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max5,
    memberName: String,
    transferSchemeName: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "totalValueTransfer.title",
      Message("totalValueTransfer.heading", transferSchemeName, memberName),
      SingleQuestion(
        form,
        QuestionField.currency(Empty)
      ),
      controllers.nonsipp.receivetransfer.routes.TotalValueTransferController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
