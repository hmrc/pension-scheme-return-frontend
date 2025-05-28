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

package controllers.nonsipp.employercontributions

import controllers.nonsipp.employercontributions.EmployerTypeOfBusinessController._
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import models.IdentityType.{Other, UKCompany, UKPartnership}
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{IdentityType, Mode}
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.employercontributions.{EmployerNamePage, EmployerTypeOfBusinessPage}
import services.SaveService
import config.RefinedTypes.{Max300, Max50}
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class EmployerTypeOfBusinessController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = EmployerTypeOfBusinessController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Int, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(EmployerNamePage(srn, memberIndex.refined, index.refined)).getOrRecoverJourney {
        employerName =>
          Ok(
            view(
              form.fromUserAnswers(EmployerTypeOfBusinessPage(srn, memberIndex.refined, index.refined)),
              viewModel(srn, memberIndex.refined, index.refined, employerName, mode)
            )
          )
      }
    }

  def onSubmit(srn: Srn, memberIndex: Int, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(EmployerNamePage(srn, memberIndex.refined, index.refined)).getOrRecoverJourney {
              employerName =>
                Future
                  .successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        viewModel(srn, memberIndex.refined, index.refined, employerName, mode)
                      )
                    )
                  )
            },
          answer => {
            for {
              updatedAnswers <- request.userAnswers
                .set(EmployerTypeOfBusinessPage(srn, memberIndex.refined, index.refined), answer)
                .mapK
              nextPage = navigator
                .nextPage(EmployerTypeOfBusinessPage(srn, memberIndex.refined, index.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, memberIndex.refined, index.refined, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
          }
        )
    }
}

object EmployerTypeOfBusinessController {

  def form(formProvider: RadioListFormProvider): Form[IdentityType] = formProvider(
    "employerTypeOfBusiness.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("employerTypeOfBusiness.radioList1"), UKCompany.name),
      RadioListRowViewModel(Message("employerTypeOfBusiness.radioList2"), UKPartnership.name),
      RadioListRowViewModel(Message("employerTypeOfBusiness.radioList3"), Other.name)
    )

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    index: Max50,
    employerName: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("employerTypeOfBusiness.title"),
      Message("employerTypeOfBusiness.heading", employerName),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.EmployerTypeOfBusinessController.onSubmit(srn, memberIndex, index, mode)
    )
}
