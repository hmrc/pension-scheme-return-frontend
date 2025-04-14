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

package controllers.nonsipp.landorproperty

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.landorproperty.{AddressLookupResultsPage, LandOrPropertyChosenAddressPage}
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.{Address, Mode}
import play.api.data.Form
import config.RefinedTypes._
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import controllers.nonsipp.landorproperty.LandPropertyAddressResultsController._
import viewmodels.DisplayMessage.{LinkMessage, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandPropertyAddressResultsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandPropertyAddressResultsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      (
        for {
          addresses <- request.userAnswers.get(AddressLookupResultsPage(srn, index)).getOrRecoverJourney
          previouslySelectedAddress = request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index))
          foundAddress = addresses.find(address => previouslySelectedAddress.exists(_.id == address.id))
          preparedForm = foundAddress.fold(form)(address => form.fill(address.id))
        } yield Ok(view(preparedForm, viewModel(srn, index, addresses, mode)))
      ).merge
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(AddressLookupResultsPage(srn, index)).getOrRecoverJourney { addresses =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, addresses, mode))))
            },
          value =>
            (
              for {
                addresses <- request.userAnswers.get(AddressLookupResultsPage(srn, index)).getOrRecoverJourneyT
                foundAddress <- addresses.find(_.id == value).getOrRecoverJourneyT
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(LandOrPropertyChosenAddressPage(srn, index), foundAddress))
                  .liftF
                nextPage = navigator.nextPage(LandOrPropertyChosenAddressPage(srn, index), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage).liftF
                _ <- saveService.save(updatedProgressAnswers).liftF
              } yield Redirect(nextPage)
            ).merge
        )
  }
}

object LandPropertyAddressResultsController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider(
    "landPropertyAddressResults.error.required"
  )

  def viewModel(srn: Srn, index: Max5000, addresses: List[Address], mode: Mode): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      title = "landPropertyAddressResults.title",
      heading = "landPropertyAddressResults.heading",
      description = Some(
        ParagraphMessage(
          "landPropertyAddressResults.description",
          LinkMessage(
            "landPropertyAddressResults.description.link",
            controllers.nonsipp.landorproperty.routes.LandPropertyAddressManualController
              .onPageLoad(srn, index, isUkAddress = true, mode)
              .url
          )
        )
      ),
      page = RadioListViewModel(
        legend = None,
        items = addresses.map(
          address =>
            RadioListRowViewModel(
              content = address.asString,
              value = address.id
            )
        )
      ),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      onSubmit =
        controllers.nonsipp.landorproperty.routes.LandPropertyAddressResultsController.onSubmit(srn, index, mode)
    )
}
