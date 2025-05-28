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

package controllers.nonsipp.bonds

import services.SaveService
import pages.nonsipp.bonds.BondsFromConnectedPartyPage
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class BondsFromConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = BondsFromConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(BondsFromConnectedPartyPage(srn, index.refined)),
          BondsFromConnectedPartyController
            .viewModel(srn, index.refined, config.urls.incomeTaxAct, mode)
        )
      )
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  BondsFromConnectedPartyController
                    .viewModel(srn, index.refined, config.urls.incomeTaxAct, mode)
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(BondsFromConnectedPartyPage(srn, index.refined), value))
              nextPage = navigator.nextPage(BondsFromConnectedPartyPage(srn, index.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(
              nextPage
            )
        )
  }
}

object BondsFromConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "bonds.connectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    incomeTaxAct: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("bonds.connectedParty.title"),
      Message("bonds.connectedParty.heading"),
      YesNoPageViewModel(
        legend = Some(Message("bonds.connectedParty.label"))
      ),
      onSubmit = controllers.nonsipp.bonds.routes.BondsFromConnectedPartyController.onSubmit(srn, index, mode)
    ).withDescription(
      ParagraphMessage("bonds.connectedParty.paragraph1") ++
        ParagraphMessage("bonds.connectedParty.paragraph2") ++
        ParagraphMessage("bonds.connectedParty.paragraph3") ++
        ListMessage(
          ListType.Bullet,
          "bonds.connectedParty.bullet1",
          "bonds.connectedParty.bullet2"
        ) ++
        ParagraphMessage(
          "bonds.connectedParty.paragraph4",
          LinkMessage(
            "bonds.connectedParty.paragraph4.link",
            incomeTaxAct,
            Map("rel" -> "noreferrer noopener", "target" -> "_blank")
          )
        )
    )
}
