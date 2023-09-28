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
import models.{ConditionalYesNo, Crn, Mode}
import navigation.Navigator
import pages.nonsipp.landorpropertydisposal.{CompanyBuyerCrnPage, CompanyBuyerNamePage}
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

class CompanyBuyerCrnController @Inject()(
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
  val form: Form[Either[String, Crn]] = CompanyBuyerCrnController.form(formProvider)
  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).sync { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex), form)
        Ok(
          view(
            preparedForm,
            CompanyBuyerCrnController.viewModel(srn, landOrPropertyIndex, disposalIndex, mode, companyName)
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
            request.usingAnswer(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).async { companyName =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      CompanyBuyerCrnController
                        .viewModel(srn, landOrPropertyIndex, disposalIndex, mode, companyName)
                    )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex), ConditionalYesNo(value))
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object CompanyBuyerCrnController {

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] =
    formProvider.conditional(
      "companyBuyerCrn.error.required",
      mappingNo = Mappings.textArea(
        "companyBuyerCrn.no.conditional.error.required",
        "companyBuyerCrn.no.conditional.error.invalid",
        "companyBuyerCrn.no.conditional.error.length"
      ),
      mappingYes = Mappings.crn(
        "companyBuyerCrn.yes.conditional.error.required",
        "companyBuyerCrn.yes.conditional.error.invalid",
        "companyBuyerCrn.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    companyName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "companyBuyerCrn.title",
      Message("companyBuyerCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("companyBuyerCrn.yes.conditional", companyName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("companyBuyerCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint("companyBuyerCrn.hint"),
      routes.CompanyBuyerCrnController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
