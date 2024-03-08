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

package controllers.nonsipp.totalvaluequotedshares

import cats.data.NonEmptyList
import cats.implicits.toShow
import config.Refined.Max3
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesCYAController._
import models.SchemeId.Srn
import models.{DateRange, Money, NormalMode, SchemeDetails}
import navigation.Navigator
import pages.nonsipp.totalvaluequotedshares._
import play.api.i18n._
import play.api.mvc._
import services.{PsrSubmissionService, SchemeDateService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage._
import viewmodels.Margin
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class TotalValueQuotedSharesCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(periods) =>
        val totalCost = request.userAnswers.get(TotalValueQuotedSharesPage(srn))
        Ok(
          view(
            viewModel(
              srn,
              totalCost.get,
              periods,
              request.schemeDetails
            )
          )
        )
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    psrSubmissionService.submitPsrDetails(srn).map {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(_) => Redirect(navigator.nextPage(TotalValueQuotedSharesCYAPage(srn), NormalMode, request.userAnswers))
    }
  }
}

object TotalValueQuotedSharesCYAController {
  def viewModel(
    srn: Srn,
    totalCost: Money,
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeDetails: SchemeDetails
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = "checkYourAnswers.title",
      heading = "checkYourAnswers.heading",
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          totalCost,
          taxYearOrAccountingPeriods,
          schemeDetails
        )
      ).withMarginBottom(Margin.Fixed60Bottom),
      refresh = None,
      buttonText = "site.saveAndContinue",
      onSubmit = controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController.onSubmit(srn)
    )

  private def sections(
    srn: Srn,
    totalCost: Money,
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeDetails: SchemeDetails
  ): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      None,
      List(
        CheckYourAnswersRowViewModel(
          Message(
            "totalValueQuotedSharesCYA.section.totalCost",
            schemeDetails.schemeName,
            taxEndDate(taxYearOrAccountingPeriods).show
          ),
          "£" + totalCost.displayAs
        ).withChangeAction(
            controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesController
              .onPageLoad(srn)
              .url,
            hidden = "totalValueQuotedSharesCYA.section.totalCost.hidden"
          )
          .withOneHalfWidth()
      )
    )
  )

  private def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }

}
