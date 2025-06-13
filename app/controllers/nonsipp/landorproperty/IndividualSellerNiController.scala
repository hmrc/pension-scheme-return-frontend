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
import forms.mappings.Mappings
import config.RefinedTypes.Max5000
import controllers.actions._
import forms.YesNoPageFormProvider
import controllers.nonsipp.landorproperty.IndividualSellerNiController._
import models.{ConditionalYesNo, Mode}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty.{IndividualSellerNiPage, LandPropertyIndividualSellersNamePage}
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IndividualSellerNiController @Inject() (
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

  private val form: Form[Either[String, Nino]] = IndividualSellerNiController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(LandPropertyIndividualSellersNamePage(srn, index)).sync { individualName =>
        val preparedForm = request.userAnswers.fillForm(IndividualSellerNiPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, individualName, mode)))
      }
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(LandPropertyIndividualSellersNamePage(srn, index)).async { individualName =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, individualName, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(IndividualSellerNiPage(srn, index), ConditionalYesNo(value))
                )
              nextPage = navigator.nextPage(IndividualSellerNiPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object IndividualSellerNiController {

  private val noFormErrors = InputFormErrors.textArea(
    "individualSellerNi.no.conditional.error.required",
    "error.textarea.invalid",
    "individualSellerNi.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Nino]] = formProvider.conditional(
    "individualSellerNi.error.required",
    mappingNo = Mappings.input(noFormErrors),
    mappingYes = Mappings.nino(
      "individualSellerNi.yes.conditional.error.required",
      "individualSellerNi.yes.conditional.error.invalid"
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "individualSellerNi.title",
      Message("individualSellerNi.heading", individualName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("individualSellerNi.yes.conditional", individualName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("individualSellerNi.no.conditional", individualName), FieldType.Textarea)
      ).withHint("individualSellerNi.hint"),
      routes.IndividualSellerNiController.onSubmit(srn, index, mode)
    )
}
