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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import models.PointOfEntry.{NoPointOfEntry, WhoWereTheSharesSoldToPointOfEntry}
import models.IdentityType._
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.actions._
import controllers.nonsipp.sharesdisposal.WhoWereTheSharesSoldToController._
import pages.nonsipp.sharesdisposal.{SharesDisposalCYAPointOfEntry, WhoWereTheSharesSoldToPage}
import navigation.Navigator
import forms.RadioListFormProvider
import models.{CheckMode, IdentityType, Mode}
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhoWereTheSharesSoldToController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = WhoWereTheSharesSoldToController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      // If this page is reached in CheckMode and there is no PointOfEntry set
      if (
        mode == CheckMode && request.userAnswers
          .get(SharesDisposalCYAPointOfEntry(srn, index, disposalIndex))
          .contains(NoPointOfEntry)
      ) {
        // Set this page as the PointOfEntry
        saveService.save(
          request.userAnswers
            .set(SharesDisposalCYAPointOfEntry(srn, index, disposalIndex), WhoWereTheSharesSoldToPointOfEntry)
            .getOrElse(request.userAnswers)
        )
      }

      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { company =>
        Ok(
          view(
            form.fromUserAnswers(WhoWereTheSharesSoldToPage(srn, index, disposalIndex)),
            viewModel(srn, index, disposalIndex, company, mode)
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
            request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { company =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(srn, index, disposalIndex, company, mode)
                    )
                  )
                )
            },
          answer =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(WhoWereTheSharesSoldToPage(srn, index, disposalIndex), answer)
              )
              nextPage = navigator.nextPage(
                WhoWereTheSharesSoldToPage(srn, index, disposalIndex),
                mode,
                updatedAnswers
              )
              updatedProgressAnswers <- saveProgress(srn, index, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object WhoWereTheSharesSoldToController {

  def form(formProvider: RadioListFormProvider): Form[IdentityType] = formProvider(
    "sharesDisposal.whoWereTheSharesSoldTo.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("sharesDisposal.whoWereTheSharesSoldTo.radioList1"), Individual.name),
      RadioListRowViewModel(Message("sharesDisposal.whoWereTheSharesSoldTo.radioList2"), UKCompany.name),
      RadioListRowViewModel(Message("sharesDisposal.whoWereTheSharesSoldTo.radioList3"), UKPartnership.name),
      RadioListRowViewModel(Message("sharesDisposal.whoWereTheSharesSoldTo.radioList4"), Other.name)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("sharesDisposal.whoWereTheSharesSoldTo.title"),
      Message("sharesDisposal.whoWereTheSharesSoldTo.heading", companyName),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.WhoWereTheSharesSoldToController.onSubmit(srn, index, disposalIndex, mode)
    )
}
