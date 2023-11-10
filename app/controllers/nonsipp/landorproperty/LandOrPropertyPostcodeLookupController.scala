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

package controllers.nonsipp.landorproperty

import cats.data.EitherT
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorproperty.LandOrPropertyPostcodeLookupController.viewModel
import forms.AddressLookupFormProvider
import forms.mappings.errors.InputFormErrors
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Address, Mode, PostcodeLookup}
import navigation.Navigator
import pages.nonsipp.landorproperty.{AddressLookupResultsPage, LandOrPropertyPostcodeLookupPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AddressService, SaveService}
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.{LinkMessage, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, PostcodeLookupViewModel}
import views.html.PostcodeLookupView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LandOrPropertyPostcodeLookupController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  addressService: AddressService,
  formProvider: AddressLookupFormProvider,
  view: PostcodeLookupView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyPostcodeLookupController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(LandOrPropertyPostcodeLookupPage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .uniqueFormErrors
        .fold(
          errs => Future.successful(BadRequest(view(errs, viewModel(srn, index, mode)))),
          value =>
            (
              for {
                addresses <- addressService.postcodeLookup(value.postcode, value.filter).liftF
                _ <- EitherT.fromEither[Future](renderErrorOnEmptyAddress(srn, index, mode, value, addresses))
                updatedUserAnswers <- Future
                  .fromTry(request.userAnswers.set(LandOrPropertyPostcodeLookupPage(srn, index), value))
                  .liftF
                updatedUserAnswersWithAddresses <- Future
                  .fromTry(
                    updatedUserAnswers.set(AddressLookupResultsPage(srn, index), addresses)
                  )
                  .liftF
                _ <- saveService.save(updatedUserAnswersWithAddresses).liftF
              } yield Redirect(
                navigator.nextPage(LandOrPropertyPostcodeLookupPage(srn, index), mode, updatedUserAnswersWithAddresses)
              )
            ).merge
        )
  }

  private def renderErrorOnEmptyAddress(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    value: PostcodeLookup,
    addresses: List[Address]
  )(
    implicit req: DataRequest[_]
  ): Either[Result, Unit] =
    Option
      .when(addresses.isEmpty) {
        val formErrs = form.fill(value).withError("postcode", "landOrPropertyPostcodeLookup.postcode.error.notFound")
        BadRequest(view(formErrs, viewModel(srn, index, mode)))
      }
      .toLeft(())
}

object LandOrPropertyPostcodeLookupController {

  private val postCodeFormErrors = InputFormErrors.postcode(
    "landOrPropertyPostcodeLookup.postcode.error.required",
    "landOrPropertyPostcodeLookup.postcode.error.invalid.characters",
    "landOrPropertyPostcodeLookup.postcode.error.invalid.format"
  )

  private val filterFormErrors = InputFormErrors.input(
    "landOrPropertyPostcodeLookup.error.required",
    "landOrPropertyPostcodeLookup.filter.error.invalid",
    "landOrPropertyPostcodeLookup.filter.error.max"
  )

  def form(formProvider: AddressLookupFormProvider): Form[PostcodeLookup] = formProvider(
    postCodeFormErrors,
    filterFormErrors
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[PostcodeLookupViewModel] =
    FormPageViewModel(
      title = "landOrPropertyPostcodeLookup.title",
      heading = "landOrPropertyPostcodeLookup.heading",
      page = PostcodeLookupViewModel(
        "landOrPropertyPostcodeLookup.lookup.label",
        "landOrPropertyPostcodeLookup.filter.label",
        ParagraphMessage(
          "landOrPropertyPostcodeLookup.paragraph",
          LinkMessage(
            "landOrPropertyPostcodeLookup.paragraph.link",
            routes.LandPropertyAddressManualController.onPageLoad(srn, index, isUkAddress = true, mode).url
          )
        )
      ),
      onSubmit = routes.LandOrPropertyPostcodeLookupController.onSubmit(srn, index, mode)
    )
}
