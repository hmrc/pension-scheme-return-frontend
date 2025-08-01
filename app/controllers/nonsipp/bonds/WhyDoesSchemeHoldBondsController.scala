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

package controllers.nonsipp.bonds

import services.SaveService
import pages.nonsipp.bonds.WhyDoesSchemeHoldBondsPage
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.toRefined5000
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{Mode, SchemeHoldBond}
import play.api.i18n.MessagesApi
import play.api.data.Form
import controllers.PSRController
import models.SchemeHoldBond._
import views.html.RadioListView
import models.SchemeId.Srn
import controllers.nonsipp.bonds.WhyDoesSchemeHoldBondsController._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhyDoesSchemeHoldBondsController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = WhyDoesSchemeHoldBondsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(WhyDoesSchemeHoldBondsPage(srn, index)),
          viewModel(srn, index, request.schemeDetails.schemeName, mode)
        )
      )

    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, index, request.schemeDetails.schemeName, mode)
                  )
                )
              ),
          answer =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(WhyDoesSchemeHoldBondsPage(srn, index), answer)
              )
              nextPage = navigator.nextPage(
                WhyDoesSchemeHoldBondsPage(srn, index),
                mode,
                updatedAnswers
              )
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(
              nextPage
            )
        )
    }
}

object WhyDoesSchemeHoldBondsController {

  def form(formProvider: RadioListFormProvider): Form[SchemeHoldBond] = formProvider[SchemeHoldBond](
    "whyDoesSchemeHoldBonds.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(
        Message("whyDoesSchemeHoldBonds.radioList1"),
        Acquisition.name,
        Message("whyDoesSchemeHoldBonds.radioList1.hint")
      ),
      RadioListRowViewModel(Message("whyDoesSchemeHoldBonds.radioList2"), Contribution.name),
      RadioListRowViewModel(Message("whyDoesSchemeHoldBonds.radioList3"), Transfer.name)
    )

  def viewModel(
    srn: Srn,
    index: Int,
    schemeName: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("whyDoesSchemeHoldBonds.title"),
      Message(
        "whyDoesSchemeHoldBonds.heading",
        schemeName
      ),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.WhyDoesSchemeHoldBondsController.onSubmit(srn, index, mode)
    )
}
