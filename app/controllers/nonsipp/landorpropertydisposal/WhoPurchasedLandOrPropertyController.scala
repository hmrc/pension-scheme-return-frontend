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

package controllers.nonsipp.landorpropertydisposal

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import models.IdentityType._
import pages.nonsipp.landorpropertydisposal.WhoPurchasedLandOrPropertyPage
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{IdentityType, Mode, NormalMode}
import play.api.i18n.MessagesApi
import play.api.data.Form
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import controllers.nonsipp.landorpropertydisposal.WhoPurchasedLandOrPropertyController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhoPurchasedLandOrPropertyController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = WhoPurchasedLandOrPropertyController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
        address =>
          Ok(
            view(
              form.fromUserAnswers(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex)),
              viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)
            )
          )
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
              address =>
                Future
                  .successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)
                      )
                    )
                  )
            },
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex), answer)
              )
              nextPage = navigator.nextPage(
                WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex),
                NormalMode,
                updatedAnswers
              )
              updatedProgressAnswers <- saveProgress(srn, landOrPropertyIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
          }
        )
    }
}

object WhoPurchasedLandOrPropertyController {

  def form(formProvider: RadioListFormProvider): Form[IdentityType] = formProvider(
    "whoPurchasedLandOrProperty.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("whoPurchasedLandOrProperty.radioList1"), Individual.name),
      RadioListRowViewModel(Message("whoPurchasedLandOrProperty.radioList2"), UKCompany.name),
      RadioListRowViewModel(Message("whoPurchasedLandOrProperty.radioList3"), UKPartnership.name),
      RadioListRowViewModel(Message("whoPurchasedLandOrProperty.radioList4"), Other.name)
    )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("whoPurchasedLandOrProperty.title"),
      Message("whoPurchasedLandOrProperty.heading", addressLine1),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.WhoPurchasedLandOrPropertyController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
