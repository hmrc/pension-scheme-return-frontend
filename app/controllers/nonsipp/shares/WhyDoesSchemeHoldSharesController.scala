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

package controllers.nonsipp.shares

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{Mode, SchemeHoldShare}
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.shares.{TypeOfSharesHeldPage, WhyDoesSchemeHoldSharesPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.PSRController
import models.SchemeHoldShare._
import controllers.nonsipp.shares.WhyDoesSchemeHoldSharesController._
import views.html.RadioListView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhyDoesSchemeHoldSharesController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = WhyDoesSchemeHoldSharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(TypeOfSharesHeldPage(srn, index)).getOrRecoverJourney { typeOfShares =>
        Ok(
          view(
            form.fromUserAnswers(WhyDoesSchemeHoldSharesPage(srn, index)),
            viewModel(srn, index, request.schemeDetails.schemeName, typeOfShares.name, mode)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(TypeOfSharesHeldPage(srn, index)).getOrRecoverJourney { typeOfShares =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(srn, index, request.schemeDetails.schemeName, typeOfShares.name, mode)
                    )
                  )
                )
            },
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(WhyDoesSchemeHoldSharesPage(srn, index), answer)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                WhyDoesSchemeHoldSharesPage(srn, index),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object WhyDoesSchemeHoldSharesController {

  def form(formProvider: RadioListFormProvider): Form[SchemeHoldShare] = formProvider[SchemeHoldShare](
    "whyDoesSchemeHoldShares.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(
        Message("whyDoesSchemeHoldShares.radioList1"),
        Acquisition.name,
        Message("whyDoesSchemeHoldShares.radioList1.hint")
      ),
      RadioListRowViewModel(Message("whyDoesSchemeHoldShares.radioList2"), Contribution.name),
      RadioListRowViewModel(Message("whyDoesSchemeHoldShares.radioList3"), Transfer.name)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    typeOfShares: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("whyDoesSchemeHoldShares.title"),
      Message(
        "whyDoesSchemeHoldShares.heading",
        schemeName,
        Message(s"whyDoesSchemeHoldShares.heading.type.$typeOfShares")
      ),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.WhyDoesSchemeHoldSharesController.onSubmit(srn, index, mode)
    )
}
