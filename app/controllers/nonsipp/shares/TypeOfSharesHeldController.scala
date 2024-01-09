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

package controllers.nonsipp.shares

import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.shares.TypeOfSharesHeldController._
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.{Mode, TypeOfShares}
import navigation.Navigator
import pages.nonsipp.shares.TypeOfSharesHeldPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TypeOfSharesHeldController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TypeOfSharesHeldController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(TypeOfSharesHeldPage(srn, index)),
          viewModel(srn, index, mode)
        )
      )
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
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
                    viewModel(srn, index, mode)
                  )
                )
              ),
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(TypeOfSharesHeldPage(srn, index), answer)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                TypeOfSharesHeldPage(srn, index),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object TypeOfSharesHeldController {

  def form(formProvider: RadioListFormProvider): Form[TypeOfShares] = formProvider[TypeOfShares](
    "typeOfShares.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("typeOfShares.radioList1"), SponsoringEmployer.name),
      RadioListRowViewModel(Message("typeOfShares.radioList2"), Unquoted.name),
      RadioListRowViewModel(Message("typeOfShares.radioList3"), ConnectedParty.name)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("typeOfShares.title"),
      Message("typeOfShares.heading"),
      RadioListViewModel(
        None,
        radioListItems,
        hint = Some(Message("typeOfShares.hint"))
      ),
      routes.TypeOfSharesHeldController.onSubmit(srn, index, mode)
    )
}
