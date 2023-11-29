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

import config.Constants
import config.Refined.{Max300, Max50}
import controllers.nonsipp.receivetransfer.TotalValueTransferController._
import controllers.PSRController
import controllers.actions._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.receivetransfer.{TotalValueTransferPage, TransferringSchemeNamePage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MoneyView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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

  private val form = TotalValueTransferController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val memberNames = request.userAnswers.membersDetails(srn)
      val transferSchemeName = request.userAnswers.get(TransferringSchemeNamePage(srn, index, secondaryIndex))
      val preparedForm = {
        request.userAnswers.get(TotalValueTransferPage(srn, index, secondaryIndex)).fold(form)(form.fill)
      }
      Ok(
        view(
          viewModel(
            srn,
            index,
            secondaryIndex,
            memberNames(index.value - 1).fullName,
            transferSchemeName.get,
            preparedForm,
            mode
          )
        )
      )
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val memberNames = request.userAnswers.membersDetails(srn)
      val transferSchemeName = request.userAnswers.get(TransferringSchemeNamePage(srn, index, secondaryIndex))
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              BadRequest(
                view(
                  viewModel(
                    srn,
                    index,
                    secondaryIndex,
                    memberNames(index.value - 1).fullName,
                    transferSchemeName.get,
                    formWithErrors,
                    mode
                  )
                )
              )
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.transformAndSet(TotalValueTransferPage(srn, index, secondaryIndex), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(TotalValueTransferPage(srn, index, secondaryIndex), mode, updatedAnswers)
            )
        )
    }
}

object TotalValueTransferController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalValueTransfer.error.required",
      "totalValueTransfer.error.invalid",
      (Constants.maxMoneyValue, "totalValueTransfer.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max50,
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
        QuestionField.input(Empty)
      ),
      controllers.nonsipp.receivetransfer.routes.TotalValueTransferController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}