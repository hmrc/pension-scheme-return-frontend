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
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.data.Form
import config.RefinedTypes.Max5000
import controllers.nonsipp.landorproperty.IsLesseeConnectedPartyController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty.{IsLesseeConnectedPartyPage, LandOrPropertyLeaseDetailsPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IsLesseeConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = IsLesseeConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(LandOrPropertyLeaseDetailsPage(srn, index)).sync { leaseName =>
        val preparedForm = request.userAnswers.fillForm(IsLesseeConnectedPartyPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, leaseName._1, mode)))
      }
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(LandOrPropertyLeaseDetailsPage(srn, index)).async { leaseName =>
              Future
                .successful(BadRequest(view(formWithErrors, viewModel(srn, index, leaseName._1, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(IsLesseeConnectedPartyPage(srn, index), value))
              nextPage = navigator.nextPage(IsLesseeConnectedPartyPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object IsLesseeConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "isLesseeConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    leaseName: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "isLesseeConnectedParty.title",
      Message("isLesseeConnectedParty.heading", leaseName),
      routes.IsLesseeConnectedPartyController.onSubmit(srn, index, mode)
    )
}
