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

package controllers.nonsipp.otherassetsdisposal

import services.SaveService
import pages.nonsipp.otherassetsdisposal.{PartnershipBuyerNamePage, PartnershipBuyerUtrPage}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.{Max50, Max5000}
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
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
  def onPageLoad(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(PartnershipBuyerNamePage(srn, index, disposalIndex)).sync { partnershipName =>
        val preparedForm =
          request.userAnswers.fillForm(PartnershipBuyerUtrPage(srn, index, disposalIndex), form)
        Ok(
          view(
            preparedForm,
            PartnershipBuyerUtrController.viewModel(srn, index, disposalIndex, mode, partnershipName)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(PartnershipBuyerNamePage(srn, index, disposalIndex)).async { partnershipName =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      PartnershipBuyerUtrController
                        .viewModel(srn, index, disposalIndex, mode, partnershipName)
                    )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(PartnershipBuyerUtrPage(srn, index, disposalIndex), ConditionalYesNo(value))
                )
              nextPage = navigator
                .nextPage(PartnershipBuyerUtrPage(srn, index, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object PartnershipBuyerUtrController {

  private val noFormErrors = InputFormErrors.textArea(
    "otherAssetsDisposal.partnershipBuyerUtr.no.conditional.error.required",
    "error.textarea.invalid",
    "otherAssetsDisposal.partnershipBuyerUtr.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Utr]] =
    formProvider.conditional(
      "otherAssetsDisposal.partnershipBuyerUtr.error.required",
      mappingNo = Mappings.input(noFormErrors),
      mappingYes = Mappings.utr(
        "otherAssetsDisposal.partnershipBuyerUtr.yes.conditional.error.required",
        "otherAssetsDisposal.partnershipBuyerUtr.yes.conditional.error.invalid",
        "otherAssetsDisposal.partnershipBuyerUtr.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    partnershipName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "otherAssetsDisposal.partnershipBuyerUtr.title",
      Message("otherAssetsDisposal.partnershipBuyerUtr.heading", partnershipName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            Message("otherAssetsDisposal.partnershipBuyerUtr.yes.conditional", partnershipName),
            Some(Message("otherAssetsDisposal.partnershipBuyerUtr.yes.conditional.hint")),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(
            Message("otherAssetsDisposal.partnershipBuyerUtr.no.conditional", partnershipName),
            FieldType.Textarea
          )
      ),
      routes.PartnershipBuyerUtrController.onSubmit(srn, index, disposalIndex, mode)
    )
}
