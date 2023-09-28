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

import config.Refined.{Max50, Max5000}
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.SchemeId.Srn
import models.{ConditionalYesNo, Mode, Utr}
import navigation.Navigator
import pages.nonsipp.landorpropertydisposal.{PartnershipBuyerNamePage, PartnershipBuyerUtrPage}
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
  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
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

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
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
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object PartnershipBuyerUtrController {

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Utr]] =
    formProvider.conditional(
      "partnershipBuyerUtr.error.required",
      mappingNo = Mappings.textArea(
        "partnershipBuyerUtr.no.conditional.error.required",
        "partnershipBuyerUtr.no.conditional.error.invalid",
        "partnershipBuyerUtr.no.conditional.error.length"
      ),
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
