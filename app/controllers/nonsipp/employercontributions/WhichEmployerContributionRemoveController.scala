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

import cats.implicits._
import com.google.inject.Inject
import config.Refined.Max50._
import config.Refined._
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.employercontributions.WhichEmployerContributionRemoveController._
import eu.timepit.refined.refineV
import forms.RadioListFormProvider
import models.Money
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.employercontributions.{EmployerNamePages, TotalEmployerContributionPages}
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel}
import views.html.ListRadiosView

import javax.inject.Named
import scala.collection.immutable.SortedMap
import scala.concurrent.ExecutionContext

class WhichEmployerContributionRemoveController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form = WhichEmployerContributionRemoveController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val totals = request.userAnswers.map(TotalEmployerContributionPages(srn, memberIndex))
    val employers = request.userAnswers.map(EmployerNamePages(srn, memberIndex))
    if (totals.size == 1) {
      refineV[OneTo50](totals.head._1.toInt + 1).fold(
        _ => Redirect(controllers.routes.UnauthorisedController.onPageLoad()),
        index =>
          Redirect(
            controllers.nonsipp.employercontributions.routes.RemoveEmployerContributionsController
              .onPageLoad(srn, memberIndex, index)
          )
      )
    } else {
      request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
        withIndexedValues(totals, employers) { sortedValues =>
          Ok(view(form, viewModel(srn, memberIndex, memberName.fullName, sortedValues)))
        }
      }
    }
  }

  def onSubmit(srn: Srn, memberIndex: Max300): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val totals = request.userAnswers.map(TotalEmployerContributionPages(srn, memberIndex))
    val employers = request.userAnswers.map(EmployerNamePages(srn, memberIndex))
    form
      .bindFromRequest()
      .fold(
        errors =>
          request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
            withIndexedValues(totals, employers)(
              sortedValues => BadRequest(view(errors, viewModel(srn, memberIndex, memberName.fullName, sortedValues)))
            )
          },
        answer =>
          Redirect(
            controllers.nonsipp.employercontributions.routes.RemoveEmployerContributionsController
              .onPageLoad(srn, memberIndex, answer)
          )
      )
  }

  private def withIndexedValues(totals: Map[String, Money], employers: Map[String, String])(
    f: Map[Int, (Money, String)] => Result
  ): Result = {
    val values = (totals, employers).tupled
    val maybeIndexedValues: Option[List[(Int, (Money, String))]] = values.toList
      .traverse { case (key, value) => key.toIntOption.map(_ -> value) }

    maybeIndexedValues match {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(indexedValues) =>
        val sortedMap = SortedMap.from(indexedValues)
        f(sortedMap)
    }
  }
}

object WhichEmployerContributionRemoveController {
  def form(formProvider: RadioListFormProvider): Form[Max50] =
    formProvider(
      "whichEmployerContributionRemove.error.required"
    )

  private def buildRows(values: Map[Int, (Money, String)]): List[ListRadiosRow] =
    values.flatMap {
      case (index, value) =>
        refineV[Max5000.Refined](index + 1).fold(
          _ => Nil,
          index =>
            List(
              ListRadiosRow(
                index.value,
                Message("whichEmployerContributionRemove.radio.label", value._1.displayAs, value._2)
              )
            )
        )
    }.toList

  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    values: Map[Int, (Money, String)]
  ): FormPageViewModel[ListRadiosViewModel] = {
    val rows = buildRows(values)

    FormPageViewModel(
      title = "whichEmployerContributionRemove.title",
      heading = Message("whichEmployerContributionRemove.heading", memberName),
      description = None,
      page = ListRadiosViewModel(
        legend = "", //TODO check a11y
        rows = rows
      ),
      refresh = None,
      buttonText = Message("site.saveAndContinue"),
      details = None,
      routes.WhichEmployerContributionRemoveController.onSubmit(srn, memberIndex)
    )
  }
}
