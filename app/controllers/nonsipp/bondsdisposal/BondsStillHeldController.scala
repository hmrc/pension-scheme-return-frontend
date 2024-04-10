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

package controllers.nonsipp.bondsdisposal

import services.SaveService
import controllers.nonsipp.bondsdisposal.BondsStillHeldController._
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import config.Constants.{maxBonds, minBondsHeld}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.IntFormProvider
import models.Mode
import play.api.data.Form
import forms.mappings.errors.IntFormErrors
import config.Refined.{Max50, Max5000}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import views.html.IntView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal.BondsStillHeldPage
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class BondsStillHeldController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: IntFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: IntView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form: Form[Int] = BondsStillHeldController.form(formProvider)

  def onPageLoad(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          viewModel(
            srn,
            bondIndex,
            disposalIndex,
            request.schemeDetails.schemeName,
            mode,
            form.fromUserAnswers(BondsStillHeldPage(srn, bondIndex, disposalIndex))
          )
        )
      )
    }

  def onSubmit(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  BondsStillHeldController
                    .viewModel(
                      srn,
                      bondIndex,
                      disposalIndex,
                      request.schemeDetails.schemeName,
                      mode,
                      formWithErrors
                    )
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(BondsStillHeldPage(srn, bondIndex, disposalIndex), answer))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(BondsStillHeldPage(srn, bondIndex, disposalIndex), mode, updatedAnswers)
            )
          }
        )
    }
}

object BondsStillHeldController {
  def form(formProvider: IntFormProvider): Form[Int] = formProvider(
    IntFormErrors(
      "bondsDisposal.bondsStillHeld.error.required",
      "bondsDisposal.bondsStillHeld.error.decimal",
      "bondsDisposal.bondsStillHeld.error.invalid.characters",
      (maxBonds, "bondsDisposal.bondsStillHeld.error.tooLarge"),
      (minBondsHeld, "bondsDisposal.bondsStillHeld.error.invalid.characters")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    mode: Mode,
    form: Form[Int]
  ): FormPageViewModel[SingleQuestion[Int]] =
    FormPageViewModel(
      title = Message("bondsDisposal.bondsStillHeld.title"),
      heading = Message("bondsDisposal.bondsStillHeld.heading", s"$schemeName"),
      page = SingleQuestion(form, QuestionField.input(Empty, Some(Message("bondsDisposal.bondsStillHeld.hint")))),
      onSubmit = routes.BondsStillHeldController.onSubmit(srn, index, disposalIndex, mode)
    )
}
