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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import utils.FormUtils._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants.{maxShares, minSharesHeld}
import controllers.actions._
import pages.nonsipp.sharesdisposal.{HowManyDisposalSharesPage, SharesDisposalProgress}
import navigation.Navigator
import forms.IntFormProvider
import models.Mode
import play.api.data.Form
import forms.mappings.errors.IntFormErrors
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.IntView
import controllers.nonsipp.sharesdisposal.HowManySharesController._
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.InputWidth
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField, SectionJourneyStatus}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class HowManySharesController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: IntFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: IntView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form: Form[Int] = HowManySharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { nameOfSharesCompany =>
        Ok(
          view(
            form.fromUserAnswers(HowManyDisposalSharesPage(srn, index, disposalIndex)),
            viewModel(
              srn,
              index,
              disposalIndex,
              nameOfSharesCompany,
              request.schemeDetails.schemeName,
              mode,
              form
            )
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { nameOfSharesCompany =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    HowManySharesController
                      .viewModel(
                        srn,
                        index,
                        disposalIndex,
                        nameOfSharesCompany,
                        request.schemeDetails.schemeName,
                        mode,
                        form
                      )
                  )
                )
              ),
            answer => {
              for {
                updatedAnswers <- request.userAnswers
                  .set(HowManyDisposalSharesPage(srn, index, disposalIndex), answer)
                  .set(SharesDisposalProgress(srn, index, disposalIndex), SectionJourneyStatus.Completed)
                  .mapK[Future]
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(HowManyDisposalSharesPage(srn, index, disposalIndex), mode, updatedAnswers)
              )
            }
          )
      }
    }
}

object HowManySharesController {
  def form(formProvider: IntFormProvider): Form[Int] = formProvider(
    IntFormErrors(
      "sharesDisposal.totalShares.error.required",
      "sharesDisposal.totalShares.error.decimal",
      "sharesDisposal.totalShares.error.invalid.characters",
      (maxShares, "sharesDisposal.totalShares.error.tooLarge"),
      (minSharesHeld, "sharesDisposal.totalShares.error.invalid.characters")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    nameOfSharesCompany: String,
    schemeName: String,
    mode: Mode,
    form: Form[Int]
  ): FormPageViewModel[SingleQuestion[Int]] =
    FormPageViewModel(
      Message("sharesDisposal.totalShares.title"),
      Message(
        "sharesDisposal.totalShares.heading",
        Message(s"$nameOfSharesCompany"),
        Message(s"$schemeName")
      ),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some(Message("sharesDisposal.totalShares.hint"))).withWidth(InputWidth.Fixed10)
      ),
      routes.HowManySharesController.onSubmit(srn, index, disposalIndex, mode)
    )
}
