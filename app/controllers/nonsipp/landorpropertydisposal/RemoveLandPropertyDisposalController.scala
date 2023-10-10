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

package controllers.nonsipp.landorpropertydisposal

import cats.implicits.{toBifunctorOps, toTraverseOps}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import eu.timepit.refined.{refineMV, refineV}
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Mode, RecipientDetails, UserAnswers}
import navigation.Navigator
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPage
import pages.nonsipp.landorpropertydisposal._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RemoveLandPropertyDisposalController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemoveLandPropertyDisposalController.form(formProvider)

  private val max: Max50 = refineMV(50)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
        address =>
          val preparedForm =
            request.userAnswers.fillForm(RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex), form)
          Ok(
            view(
              preparedForm,
              RemoveLandPropertyDisposalController
                .viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)
            )
          )
      }
    }

  private def buildIndexes(num: Int): Try[List[Max50]] =
    (1 to num).map(i => refineV[Max50.Refined](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllLandOrProperty(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    userAnswers: UserAnswers
  ): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, disposalIndex) =>
          ua.flatMap(_.remove(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex)))
            .flatMap(_.remove(LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex)))
            .flatMap(_.remove(WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex)))
            .flatMap(_.remove(LandOrPropertyDisposalSellerConnectedPartyPage(srn, landOrPropertyIndex, disposalIndex)))
            .flatMap(_.remove(DisposalIndependentValuationPage(srn, landOrPropertyIndex, disposalIndex)))
            .flatMap(_.remove(TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex)))
            .flatMap(_.remove(RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex)))

      }
    } yield updatedUserAnswers

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
              address =>
                Future.successful(
                  BadRequest(
                    view(
                      errors,
                      RemoveLandPropertyDisposalController
                        .viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)
                    )
                  )
                )
            },
          value =>
            if (value) {
              for {
                removedUserAnswers <- Future
                  .fromTry(removeAllLandOrProperty(srn, landOrPropertyIndex, request.userAnswers))
                updatedUserAnswers <- Future.fromTry(
                  updateUserAnswersWithLandOrProperties(
                    landOrPropertyIndex.value,
                    srn,
                    landOrPropertyIndex,
                    removedUserAnswers
                  )
                )
                _ <- saveService.save(updatedUserAnswers)
              } yield {
                Redirect(
                  navigator.nextPage(
                    RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex),
                    mode,
                    updatedUserAnswers
                  )
                )
              }
            } else {
              Future
                .successful(
                  Redirect(
                    navigator.nextPage(
                      RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex),
                      mode,
                      request.userAnswers
                    )
                  )
                )
            }
        )
    }

  private val recipientDetails = RecipientDetails("test name", "test description")

  private def updateUserAnswersWithLandOrProperties(
    num: Int,
    srn: Srn,
    index: Max5000,
    userAnswers: UserAnswers
  ): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      otherBuyer = indexes.map(disposalIndex => OtherBuyerDetailsPage(srn, index, disposalIndex) -> recipientDetails)
      stillHeld = indexes.map(disposalIndex => LandOrPropertyStillHeldPage(srn, index, disposalIndex) -> true)
      ua1 <- otherBuyer.foldLeft(Try(userAnswers)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua2 <- stillHeld.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
    } yield ua2

}

object RemoveLandPropertyDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeLandPropertyDisposal.error.required"
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "removeLandPropertyDisposal.title",
      Message("removeLandPropertyDisposal.heading", addressLine1),
      routes.RemoveLandPropertyDisposalController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
