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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.{Max50, Max5000}
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import pages.nonsipp.landorpropertydisposal.{PartnershipBuyerNamePage, PartnershipBuyerUtrPage}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Mode, Utr}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PartnershipBuyerUtrController @Inject() (
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
  val form: Form[Either[String, Utr]] = PartnershipBuyerUtrController.form(formProvider)
  def onPageLoad(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).sync { partnershipName =>
        val preparedForm =
          request.userAnswers.fillForm(PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex), form)
        Ok(
          view(
            preparedForm,
            PartnershipBuyerUtrController.viewModel(srn, landOrPropertyIndex, disposalIndex, mode, partnershipName)
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
            request.usingAnswer(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).async {
              partnershipName =>
                Future
                  .successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        PartnershipBuyerUtrController
                          .viewModel(srn, landOrPropertyIndex, disposalIndex, mode, partnershipName)
                      )
                    )
                  )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex), ConditionalYesNo(value))
                )
              nextPage = navigator
                .nextPage(PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, landOrPropertyIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object PartnershipBuyerUtrController {

  private val noFormErrors = InputFormErrors.textArea(
    "partnershipBuyerUtr.no.conditional.error.required",
    "error.textarea.invalid",
    "partnershipBuyerUtr.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Utr]] =
    formProvider.conditional(
      "partnershipBuyerUtr.error.required",
      mappingNo = Mappings.input(noFormErrors),
      mappingYes = Mappings.utr(
        "partnershipBuyerUtr.yes.conditional.error.required",
        "partnershipBuyerUtr.yes.conditional.error.invalid",
        "partnershipBuyerUtr.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    partnershipName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "partnershipBuyerUtr.title",
      Message("partnershipBuyerUtr.heading", partnershipName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("partnershipBuyerUtr.yes.conditional", partnershipName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("partnershipBuyerUtr.no.conditional", partnershipName), FieldType.Textarea)
      ),
      routes.PartnershipBuyerUtrController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
