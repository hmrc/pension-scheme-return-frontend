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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.{Max50, Max5000}
import controllers.actions._
import pages.nonsipp.sharesdisposal.{IndividualBuyerNinoNumberPage, SharesIndividualBuyerNamePage}
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Mode}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.nonsipp.sharesdisposal.IndividualBuyerNinoNumberController._
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IndividualBuyerNinoNumberController @Inject() (
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

  def onPageLoad(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(SharesIndividualBuyerNamePage(srn, index, disposalIndex)).sync { individualName =>
        val preparedForm =
          request.userAnswers.fillForm(IndividualBuyerNinoNumberPage(srn, index, disposalIndex), form)
        Ok(view(preparedForm, viewModel(srn, index, disposalIndex, individualName, mode)))
      }

    }

  def onSubmit(srn: Srn, shares: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(SharesIndividualBuyerNamePage(srn, shares, disposalIndex)).async { individualName =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, shares, disposalIndex, individualName, mode))
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(
                      IndividualBuyerNinoNumberPage(srn, shares, disposalIndex),
                      ConditionalYesNo(value)
                    )
                )
              nextPage = navigator
                .nextPage(IndividualBuyerNinoNumberPage(srn, shares, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, shares, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object IndividualBuyerNinoNumberController {

  private val noFormErrors = InputFormErrors.textArea(
    "individualBuyerNinoNumber.no.conditional.error.required",
    "error.textarea.invalid",
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
    index: Max5000,
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
      ).withHint("individualBuyerNinoNumber.hint"),
      routes.IndividualBuyerNinoNumberController.onSubmit(srn, index, disposalIndex, mode)
    )
}
