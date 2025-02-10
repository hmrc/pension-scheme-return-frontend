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

package controllers.nonsipp.moneyborrowed

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.data.Form
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.TextAreaView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.moneyborrowed.{BorrowedAmountAndRatePage, LenderNamePage, WhySchemeBorrowedMoneyPage}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextAreaViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhySchemeBorrowedMoneyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextAreaView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val form = WhySchemeBorrowedMoneyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney { lenderName =>
        request.userAnswers.get(BorrowedAmountAndRatePage(srn, index)).getOrRecoverJourney { amountBorrowed =>
          val preparedForm = {
            request.userAnswers.fillForm(WhySchemeBorrowedMoneyPage(srn, index), form)
          }
          Ok(
            view(
              preparedForm,
              WhySchemeBorrowedMoneyController
                .viewModel(
                  srn,
                  index,
                  mode,
                  request.schemeDetails.schemeName,
                  amountBorrowed._1.displayAs,
                  lenderName
                )
            )
          )
        }

      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney { lenderName =>
        request.userAnswers.get(BorrowedAmountAndRatePage(srn, index)).getOrRecoverJourney { amountBorrowed =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      WhySchemeBorrowedMoneyController.viewModel(
                        srn,
                        index,
                        mode,
                        request.schemeDetails.schemeName,
                        amountBorrowed._1.displayAs,
                        lenderName
                      )
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(WhySchemeBorrowedMoneyPage(srn, index), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(
                  navigator.nextPage(WhySchemeBorrowedMoneyPage(srn, index), mode, updatedAnswers)
                )
            )
        }
      }

  }
}

object WhySchemeBorrowedMoneyController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "moneyBorrowed.WhyBorrowed.error.required",
    "moneyBorrowed.WhyBorrowed.error.length",
    "error.textarea.invalid"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    schemeName: String,
    amountBorrowed: String,
    lenderName: String
  ): FormPageViewModel[TextAreaViewModel] =
    FormPageViewModel(
      "moneyBorrowed.WhyBorrowed.title",
      Message("moneyBorrowed.WhyBorrowed.heading", schemeName, lenderName, amountBorrowed),
      TextAreaViewModel(),
      controllers.nonsipp.moneyborrowed.routes.WhySchemeBorrowedMoneyController.onSubmit(srn, index, mode)
    )
}
