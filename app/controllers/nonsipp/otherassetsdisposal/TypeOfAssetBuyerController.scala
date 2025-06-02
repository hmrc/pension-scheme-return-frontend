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
import pages.nonsipp.otherassetsdisposal.TypeOfAssetBuyerPage
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import models.IdentityType._
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{IdentityType, Mode}
import play.api.data.Form
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import controllers.nonsipp.otherassetsdisposal.TypeOfAssetBuyerController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TypeOfAssetBuyerController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TypeOfAssetBuyerController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex)),
          viewModel(srn, assetIndex, disposalIndex, mode)
        )
      )
    }

  def onSubmit(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, assetIndex, disposalIndex, mode)
                  )
                )
              ),
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), answer)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object TypeOfAssetBuyerController {

  def form(formProvider: RadioListFormProvider): Form[IdentityType] = formProvider(
    "typeOfAssetBuyer.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("typeOfAssetBuyer.radioList1"), Individual.name),
      RadioListRowViewModel(Message("typeOfAssetBuyer.radioList2"), UKCompany.name),
      RadioListRowViewModel(Message("typeOfAssetBuyer.radioList3"), UKPartnership.name),
      RadioListRowViewModel(Message("typeOfAssetBuyer.radioList4"), Other.name)
    )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("typeOfAssetBuyer.title"),
      Message("typeOfAssetBuyer.heading"),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.TypeOfAssetBuyerController.onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
