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
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import config.Constants.{maxShares, minShares}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.HowManySharesSoldPage
import navigation.Navigator
import forms.IntFormProvider
import models.Mode
import play.api.data.Form
import forms.mappings.errors.IntFormErrors
import controllers.nonsipp.sharesdisposal.HowManySharesSoldController._
import utils.FormUtils._
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.IntView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import viewmodels.InputWidth
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class HowManySharesSoldController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: IntFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: IntView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form: Form[Int] = HowManySharesSoldController.form(formProvider)

  def onPageLoad(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
        Ok(
          view(
            form.fromUserAnswers(HowManySharesSoldPage(srn, shareIndex, disposalIndex)),
            viewModel(
              srn,
              shareIndex,
              disposalIndex,
              companyName,
              mode,
              form
            )
          )
        )
      }
    }

  def onSubmit(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, shareIndex, disposalIndex, companyName, mode, form)
                  )
                )
              ),
            answer =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(HowManySharesSoldPage(srn, shareIndex, disposalIndex), answer))
                nextPage = navigator
                  .nextPage(HowManySharesSoldPage(srn, shareIndex, disposalIndex), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, shareIndex, disposalIndex, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
          )
      }
    }
}

object HowManySharesSoldController {
  def form(formProvider: IntFormProvider): Form[Int] =
    formProvider(
      IntFormErrors(
        "sharesDisposal.howManySharesSold.error.required",
        "sharesDisposal.howManySharesSold.error.decimal",
        "sharesDisposal.howManySharesSold.error.invalid.characters",
        (maxShares, "sharesDisposal.howManySharesSold.error.size"),
        (minShares, "sharesDisposal.howManySharesSold.error.zero")
      )
    )

  def viewModel(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    mode: Mode,
    form: Form[Int]
  ): FormPageViewModel[SingleQuestion[Int]] =
    FormPageViewModel(
      title = Message("sharesDisposal.howManySharesSold.title"),
      heading = Message("sharesDisposal.howManySharesSold.heading", companyName),
      page = SingleQuestion(form, QuestionField.input(Empty).withWidth(InputWidth.Fixed10)),
      onSubmit = routes.HowManySharesSoldController.onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
