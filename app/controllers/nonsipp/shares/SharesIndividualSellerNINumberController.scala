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

package controllers.nonsipp.shares

import services.SaveService
import viewmodels.implicits._
import forms.mappings.Mappings
import config.RefinedTypes.Max5000
import controllers.actions._
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Mode}
import forms.mappings.errors.InputFormErrors
import pages.nonsipp.shares.{IndividualNameOfSharesSellerPage, SharesIndividualSellerNINumberPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._
import controllers.nonsipp.shares.SharesIndividualSellerNINumberController._
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SharesIndividualSellerNINumberController @Inject()(
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

  private val form: Form[Either[String, Nino]] = SharesIndividualSellerNINumberController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(IndividualNameOfSharesSellerPage(srn, index)).sync { individualName =>
        val preparedForm =
          request.userAnswers.fillForm(SharesIndividualSellerNINumberPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, individualName, mode)))
      }

    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(IndividualNameOfSharesSellerPage(srn, index)).async { individualName =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, index, individualName, mode))
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(
                      SharesIndividualSellerNINumberPage(srn, index),
                      ConditionalYesNo(value)
                    )
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(SharesIndividualSellerNINumberPage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object SharesIndividualSellerNINumberController {

  private val noFormErrors = InputFormErrors.textArea(
    "sharesIndividualSellerNINumber.no.conditional.error.required",
    "error.textarea.invalid",
    "sharesIndividualSellerNINumber.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Nino]] = formProvider.conditional(
    "sharesIndividualSellerNINumber.error.required",
    mappingNo = Mappings.input(noFormErrors),
    mappingYes = Mappings.nino(
      "sharesIndividualSellerNINumber.yes.conditional.error.required",
      "sharesIndividualSellerNINumber.yes.conditional.error.invalid"
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "sharesIndividualSellerNINumber.title",
      Message("sharesIndividualSellerNINumber.heading", individualName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("sharesIndividualSellerNINumber.yes.conditional", individualName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("sharesIndividualSellerNINumber.no.conditional", individualName), FieldType.Textarea)
      ).withHint("sharesIndividualSellerNINumber.hint"),
      routes.SharesIndividualSellerNINumberController.onSubmit(srn, index, mode)
    )
}
