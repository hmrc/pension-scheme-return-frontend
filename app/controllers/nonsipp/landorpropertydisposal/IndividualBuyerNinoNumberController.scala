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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.Refined.{Max50, Max5000}
import pages.nonsipp.landorpropertydisposal.{IndividualBuyerNinoNumberPage, LandOrPropertyIndividualBuyerNamePage}
import controllers.actions._
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Mode}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import viewmodels.implicits._
import controllers.nonsipp.landorpropertydisposal.IndividualBuyerNinoNumberController._
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IndividualBuyerNinoNumberController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConditionalYesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form: Form[Either[String, Nino]] = IndividualBuyerNinoNumberController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).sync {
        individualName =>
          val preparedForm =
            request.userAnswers.fillForm(IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex), form)
          Ok(view(preparedForm, viewModel(srn, landOrPropertyIndex, disposalIndex, individualName, mode)))
      }

    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).async {
              individualName =>
                Future.successful(
                  BadRequest(
                    view(formWithErrors, viewModel(srn, landOrPropertyIndex, disposalIndex, individualName, mode))
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(
                      IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex),
                      ConditionalYesNo(value)
                    )
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object IndividualBuyerNinoNumberController {

  private val noFormErrors = InputFormErrors.textArea(
    "individualBuyerNinoNumber.no.conditional.error.required",
    "individualBuyerNinoNumber.no.conditional.error.invalid",
    "individualBuyerNinoNumber.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Nino]] = formProvider.conditional(
    "individualBuyerNinoNumber.error.required",
    mappingNo = Mappings.input(noFormErrors),
    mappingYes = Mappings.nino(
      "individualBuyerNinoNumber.yes.conditional.error.required",
      "individualBuyerNinoNumber.yes.conditional.error.invalid"
    )
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    individualName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "individualBuyerNinoNumber.title",
      Message("individualBuyerNinoNumber.heading", individualName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("individualBuyerNinoNumber.yes.conditional", individualName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("individualBuyerNinoNumber.no.conditional", individualName), FieldType.Textarea)
      ),
      routes.IndividualBuyerNinoNumberController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
