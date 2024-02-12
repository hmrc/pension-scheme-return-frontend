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

package controllers.nonsipp.sharesdisposal

import config.Refined.{Max50, Max5000}
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.SchemeId.Srn
import models.{ConditionalYesNo, Mode, Utr}
import navigation.Navigator
import pages.nonsipp.sharesdisposal.{PartnershipBuyerNamePage, PartnershipBuyerUtrPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PartnershipBuyerUtrController @Inject()(
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
  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
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

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
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
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(PartnershipBuyerUtrPage(srn, index, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object PartnershipBuyerUtrController {

  private val noFormErrors = InputFormErrors.textArea(
    "sharesDisposal.partnershipBuyerUtr.no.conditional.error.required",
    "sharesDisposal.partnershipBuyerUtr.no.conditional.error.invalid",
    "sharesDisposal.partnershipBuyerUtr.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Utr]] =
    formProvider.conditional(
      "sharesDisposal.partnershipBuyerUtr.error.required",
      mappingNo = Mappings.input(noFormErrors),
      mappingYes = Mappings.utr(
        "sharesDisposal.partnershipBuyerUtr.yes.conditional.error.required",
        "sharesDisposal.partnershipBuyerUtr.yes.conditional.error.invalid",
        "sharesDisposal.partnershipBuyerUtr.yes.conditional.error.length"
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
      "sharesDisposal.partnershipBuyerUtr.title",
      Message("sharesDisposal.partnershipBuyerUtr.heading", partnershipName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("sharesDisposal.partnershipBuyerUtr.yes.conditional", partnershipName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(
            Message("sharesDisposal.partnershipBuyerUtr.no.conditional", partnershipName),
            FieldType.Textarea
          )
      ),
      routes.PartnershipBuyerUtrController.onSubmit(srn, index, disposalIndex, mode)
    )
}
