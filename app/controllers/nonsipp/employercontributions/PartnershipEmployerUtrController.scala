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

package controllers.nonsipp.employercontributions

import config.Refined.{Max300, Max50}
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.employercontributions.PartnershipEmployerUtrController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.SchemeId.Srn
import models.{ConditionalYesNo, Mode, Utr}
import navigation.Navigator
import pages.nonsipp.employercontributions.{EmployerNamePage, PartnershipEmployerUtrPage}
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

class PartnershipEmployerUtrController @Inject()(
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
  val form: Form[Either[String, Utr]] = PartnershipEmployerUtrController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(EmployerNamePage(srn: Srn, index, secondaryIndex)).sync { employerName =>
        val preparedForm =
          request.userAnswers.fillForm(PartnershipEmployerUtrPage(srn, index, secondaryIndex), form)
        Ok(view(preparedForm, viewModel(srn, index, secondaryIndex, mode, employerName)))
      }

    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(EmployerNamePage(srn: Srn, index, secondaryIndex)).async { employerName =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, index, secondaryIndex, mode, employerName))
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(PartnershipEmployerUtrPage(srn, index, secondaryIndex), ConditionalYesNo(value))
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(PartnershipEmployerUtrPage(srn, index, secondaryIndex), mode, updatedAnswers)
            )
        )
    }
}

object PartnershipEmployerUtrController {

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Utr]] =
    formProvider.conditional(
      "partnershipEmployerUtr.error.required",
      mappingNo = Mappings.textArea(
        "partnershipEmployerUtr.no.conditional.error.required",
        "partnershipEmployerUtr.no.conditional.error.invalid",
        "partnershipEmployerUtr.no.conditional.error.length"
      ),
      mappingYes = Mappings.utr(
        "partnershipEmployerUtr.yes.conditional.error.required",
        "partnershipEmployerUtr.yes.conditional.error.invalid"
      )
    )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max50,
    mode: Mode,
    employerName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "partnershipEmployerUtr.title",
      Message("partnershipEmployerUtr.heading", employerName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("partnershipEmployerUtr.yes.conditional", employerName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("partnershipEmployerUtr.no.conditional", employerName), FieldType.Textarea)
      ),
      routes.PartnershipEmployerUtrController.onSubmit(srn, index, secondaryIndex, mode)
    )
}
