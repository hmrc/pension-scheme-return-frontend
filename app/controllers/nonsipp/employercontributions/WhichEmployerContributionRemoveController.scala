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

import pages.nonsipp.employercontributions.{
  EmployerContributionsProgress,
  EmployerNamePage,
  TotalEmployerContributionPage
}
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.mvc._
import com.google.inject.Inject
import utils.ListUtils.ListOps
import utils.IntUtils.{toInt, IntOpts}
import cats.implicits._
import controllers.actions._
import config.RefinedTypes.Max50._
import forms.RadioListFormProvider
import models.{Money, NormalMode}
import play.api.i18n.MessagesApi
import viewmodels.implicits._
import controllers.nonsipp.employercontributions.WhichEmployerContributionRemoveController._
import config.RefinedTypes._
import controllers.PSRController
import views.html.ListRadiosView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel}
import models.requests.DataRequest
import play.api.data.Form

class WhichEmployerContributionRemoveController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max50] = WhichEmployerContributionRemoveController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val completed: List[Max50] = request.userAnswers
      .map(EmployerContributionsProgress.all(srn, memberIndex.refined))
      .filter {
        case (_, status) => status.completed
      }
      .keys
      .toList
      .refine[Max50.Refined]

    completed match {
      case Nil =>
        Redirect(
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoad(srn, page = 1, NormalMode)
        )
      case head :: Nil =>
        Redirect(
          controllers.nonsipp.employercontributions.routes.RemoveEmployerContributionsController
            .onPageLoad(srn, memberIndex, head)
        )
      case _ =>
        (
          for {
            memberName <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex.refined)).getOrRecoverJourney
            values <- getJourneyValues(srn, memberIndex.refined)
          } yield Ok(view(form, viewModel(srn, memberIndex.refined, memberName.fullName, values)))
        ).merge
    }
  }

  def onSubmit(srn: Srn, memberIndex: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    form
      .bindFromRequest()
      .fold(
        errors =>
          (
            for {
              memberName <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex.refined)).getOrRecoverJourney
              values <- getJourneyValues(srn, memberIndex.refined)
            } yield BadRequest(view(errors, viewModel(srn, memberIndex.refined, memberName.fullName, values)))
          ).merge,
        answer =>
          Redirect(
            controllers.nonsipp.employercontributions.routes.RemoveEmployerContributionsController
              .onPageLoad(srn, memberIndex, answer)
          )
      )
  }

  private def getJourneyValues(srn: Srn, memberIndex: Max300)(
    implicit request: DataRequest[_]
  ): Either[Result, List[(Max50, Money, String)]] =
    request.userAnswers
      .map(EmployerContributionsProgress.all(srn, memberIndex))
      .filter {
        case (_, status) => status.completed
      }
      .keys
      .toList
      .refine[Max50.Refined]
      .traverse { secondaryIndex =>
        for {
          totalContribution <- request.userAnswers
            .get(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex))
            .getOrRecoverJourney
          employerName <- request.userAnswers
            .get(EmployerNamePage(srn, memberIndex, secondaryIndex))
            .getOrRecoverJourney
        } yield (secondaryIndex, totalContribution, employerName)
      }
}

object WhichEmployerContributionRemoveController {
  def form(formProvider: RadioListFormProvider): Form[Max50] =
    formProvider(
      "whichEmployerContributionRemove.error.required"
    )

  private def buildRows(values: List[(Max50, Money, String)]): List[ListRadiosRow] =
    values.flatMap {
      case (index, total, employerName) =>
        List(
          ListRadiosRow(
            index.value,
            Message("whichEmployerContributionRemove.radio.label", total.displayAs, employerName)
          )
        )
    }

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    values: List[(Max50, Money, String)]
  ): FormPageViewModel[ListRadiosViewModel] =
    FormPageViewModel(
      title = "whichEmployerContributionRemove.title",
      heading = Message("whichEmployerContributionRemove.heading", memberName),
      description = None,
      page = ListRadiosViewModel(
        legend = None,
        rows = buildRows(values)
      ),
      refresh = None,
      buttonText = Message("site.saveAndContinue"),
      details = None,
      routes.WhichEmployerContributionRemoveController.onSubmit(srn, memberIndex)
    )
}
