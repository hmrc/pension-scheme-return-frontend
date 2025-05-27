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
import utils.IntUtils.{toInt, IntOpts}
import config.Constants.{maxTotalConsiderationAmount, minTotalConsiderationAmount}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.{HowManySharesSoldPage, TotalConsiderationSharesSoldPage}
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import forms.mappings.errors.MoneyFormErrors
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}
import controllers.nonsipp.sharesdisposal.TotalConsiderationSharesSoldController._
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalConsiderationSharesSoldController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalConsiderationSharesSoldController.form(formProvider)

  def onPageLoad(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex.refined)).getOrRecoverJourney {
        companyName =>
          request.userAnswers
            .get(HowManySharesSoldPage(srn, shareIndex.refined, disposalIndex.refined))
            .getOrRecoverJourney { numShares =>
              val preparedForm =
                request.userAnswers
                  .fillForm(TotalConsiderationSharesSoldPage(srn, shareIndex.refined, disposalIndex.refined), form)

              Ok(
                view(
                  preparedForm,
                  viewModel(srn, shareIndex.refined, disposalIndex.refined, numShares, companyName, form, mode)
                )
              )
            }
      }
    }

  def onSubmit(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex.refined)).getOrRecoverJourney {
        companyName =>
          request.userAnswers
            .get(HowManySharesSoldPage(srn, shareIndex.refined, disposalIndex.refined))
            .getOrRecoverJourney { numShares =>
              form
                .bindFromRequest()
                .fold(
                  formWithErrors => {
                    Future.successful(
                      BadRequest(
                        view(
                          formWithErrors,
                          viewModel(srn, shareIndex.refined, disposalIndex.refined, numShares, companyName, form, mode)
                        )
                      )
                    )
                  },
                  value =>
                    for {
                      updatedAnswers <- Future
                        .fromTry(
                          request.userAnswers.set(
                            TotalConsiderationSharesSoldPage(srn, shareIndex.refined, disposalIndex.refined),
                            value
                          )
                        )
                      nextPage = navigator
                        .nextPage(
                          TotalConsiderationSharesSoldPage(srn, shareIndex.refined, disposalIndex.refined),
                          mode,
                          updatedAnswers
                        )
                      updatedProgressAnswers <- saveProgress(
                        srn,
                        shareIndex.refined,
                        disposalIndex.refined,
                        updatedAnswers,
                        nextPage
                      )
                      _ <- saveService.save(updatedProgressAnswers)
                    } yield Redirect(nextPage)
                )
            }
      }
    }
}

object TotalConsiderationSharesSoldController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      requiredKey = "sharesDisposal.totalConsiderationSharesSold.error.required",
      nonNumericKey = "sharesDisposal.totalConsiderationSharesSold.error.invalid.characters",
      min = (minTotalConsiderationAmount, "sharesDisposal.totalConsiderationSharesSold.error.tooSmall"),
      max = (maxTotalConsiderationAmount, "sharesDisposal.totalConsiderationSharesSold.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    numShares: Int,
    companyName: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      title = Message("sharesDisposal.totalConsiderationSharesSold.title"),
      heading = Message("sharesDisposal.totalConsiderationSharesSold.heading", numShares, companyName),
      page = SingleQuestion(form, QuestionField.currency(Empty)),
      onSubmit = routes.TotalConsiderationSharesSoldController.onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
