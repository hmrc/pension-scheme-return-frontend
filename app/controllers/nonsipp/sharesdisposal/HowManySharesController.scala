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

package controllers.nonsipp.sharesdisposal

import config.Constants.{maxShares, minSharesHeld}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.sharesdisposal.HowManySharesController._
import forms.IntFormProvider
import forms.mappings.errors.IntFormErrors
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import pages.nonsipp.sharesdisposal.HowManySharesPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.InputWidth
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.IntView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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

  private def form = HowManySharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { nameOfSharesCompany =>
        Ok(
          view(
            viewModel(
              srn,
              index,
              disposalIndex,
              nameOfSharesCompany,
              request.schemeDetails.schemeName,
              mode,
              form.fromUserAnswers(HowManySharesPage(srn, index, disposalIndex))
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
                    HowManySharesController
                      .viewModel(
                        srn,
                        index,
                        disposalIndex,
                        nameOfSharesCompany,
                        request.schemeDetails.schemeName,
                        mode,
                        formWithErrors
                      )
                  )
                )
              ),
            answer => {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(HowManySharesPage(srn, index, disposalIndex), answer))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(HowManySharesPage(srn, index, disposalIndex), mode, updatedAnswers))
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
