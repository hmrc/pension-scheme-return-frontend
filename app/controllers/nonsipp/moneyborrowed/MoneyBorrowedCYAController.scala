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

package controllers.nonsipp.moneyborrowed

import services.PsrSubmissionService
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.Max5000
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import controllers.nonsipp.moneyborrowed.MoneyBorrowedCYAController._
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n._
import pages.nonsipp.moneyborrowed._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.ExecutionContext

import java.time.LocalDate
import javax.inject.{Inject, Named}

class MoneyBorrowedCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Max5000,
    checkOrChange: CheckOrChange
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {

          lenderName <- request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney
          lenderConnectedParty <- request.userAnswers.get(IsLenderConnectedPartyPage(srn, index)).getOrRecoverJourney
          borrowedAmountAndRate <- request.userAnswers.get(BorrowedAmountAndRatePage(srn, index)).getOrRecoverJourney
          whenBorrowed <- request.userAnswers.get(WhenBorrowedPage(srn, index)).getOrRecoverJourney
          schemeAssets <- request.userAnswers
            .get(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index))
            .getOrRecoverJourney
          schemeBorrowed <- request.userAnswers.get(WhySchemeBorrowedMoneyPage(srn, index)).getOrRecoverJourney

          schemeName = request.schemeDetails.schemeName
        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                lenderName,
                lenderConnectedParty,
                borrowedAmountAndRate,
                whenBorrowed,
                schemeAssets,
                schemeBorrowed,
                checkOrChange
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Max5000, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService
        .submitPsrDetails(
          srn,
          optFallbackCall = Some(
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
              .onPageLoad(srn, index, checkOrChange)
          )
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) => Redirect(navigator.nextPage(MoneyBorrowedCYAPage(srn), NormalMode, request.userAnswers))
        }
    }
}

case class ViewModelParameters(
  srn: Srn,
  index: Max5000,
  schemeName: String,
  lenderName: String,
  lenderConnectedParty: Boolean,
  borrowedAmountAndRate: (Money, Percentage),
  whenBorrowed: LocalDate,
  schemeAssets: Money,
  schemeBorrowed: String,
  checkOrChange: CheckOrChange
)
object MoneyBorrowedCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.checkOrChange
        .fold(check = "moneyBorrowedCheckYourAnswers.title", change = "moneyBorrowedCheckYourAnswers.change.title"),
      heading = parameters.checkOrChange.fold(
        check = "moneyBorrowedCheckYourAnswers.heading",
        change = Message(
          "moneyBorrowedCheckYourAnswers.change.heading",
          parameters.borrowedAmountAndRate._1.displayAs,
          parameters.lenderName
        )
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.index,
          parameters.schemeName,
          parameters.lenderName,
          parameters.lenderConnectedParty,
          parameters.borrowedAmountAndRate,
          parameters.whenBorrowed,
          parameters.schemeAssets,
          parameters.schemeBorrowed,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.checkOrChange.fold(check = "site.saveAndContinue", change = "site.continue"),
      onSubmit = routes.MoneyBorrowedCYAController.onSubmit(parameters.srn, parameters.index, parameters.checkOrChange)
    )

  private def sections(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    lenderName: String,
    lenderConnectedParty: Boolean,
    borrowedAmountAndRate: (Money, Percentage),
    whenBorrowed: LocalDate,
    schemeAssets: Money,
    schemeBorrowed: String,
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val (borrowedAmount, borrowedInterestRate) = borrowedAmountAndRate

    checkYourAnswerSection(
      srn,
      index,
      schemeName,
      lenderName,
      lenderConnectedParty,
      borrowedAmount,
      borrowedInterestRate,
      whenBorrowed,
      schemeAssets,
      schemeBorrowed,
      mode
    )
  }

  private def checkYourAnswerSection(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    lenderName: String,
    lenderConnectedParty: Boolean,
    borrowedAmount: Money,
    borrowedInterestRate: Percentage,
    whenBorrowed: LocalDate,
    schemeAssets: Money,
    schemeBorrowed: String,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel("moneyBorrowedCheckYourAnswers.section.lenderName", lenderName.show)
            .withAction(
              SummaryAction(
                "site.change",
                routes.LenderNameController.onSubmit(srn, index, mode).url
              ).withVisuallyHiddenContent("moneyBorrowedCheckYourAnswers.section.lenderName.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("moneyBorrowedCheckYourAnswers.section.lenderConnectedParty", lenderName.show),
            if (lenderConnectedParty) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.IsLenderConnectedPartyController.onSubmit(srn, index, mode).url + "#connected"
            ).withVisuallyHiddenContent("moneyBorrowedCheckYourAnswers.section.lenderConnectedParty.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("moneyBorrowedCheckYourAnswers.section.borrowedAmount"),
            s"£${borrowedAmount.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.moneyborrowed.routes.BorrowedAmountAndRateController
                .onSubmit(srn, index, mode)
                .url + "#amount"
            ).withVisuallyHiddenContent(
              Message("moneyBorrowedCheckYourAnswers.section.borrowedAmount.hidden")
            )
          ),
          CheckYourAnswersRowViewModel(
            "moneyBorrowedCheckYourAnswers.section.borrowedRate",
            s"${borrowedInterestRate.displayAs}%"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.moneyborrowed.routes.BorrowedAmountAndRateController.onSubmit(srn, index, mode).url
            ).withVisuallyHiddenContent("moneyBorrowedCheckYourAnswers.section.borrowedRate.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("moneyBorrowedCheckYourAnswers.section.whenBorrowed", schemeName),
            whenBorrowed.show
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.moneyborrowed.routes.WhenBorrowedController.onSubmit(srn, index, mode).url
            ).withVisuallyHiddenContent("moneyBorrowedCheckYourAnswers.section.whenBorrowed.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("moneyBorrowedCheckYourAnswers.section.schemeAssets", schemeName, whenBorrowed.show),
            s"£${schemeAssets.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.ValueOfSchemeAssetsWhenMoneyBorrowedController.onSubmit(srn, index, mode).url
            ).withVisuallyHiddenContent("moneyBorrowedCheckYourAnswers.section.schemeAssets.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("moneyBorrowedCheckYourAnswers.section.schemeBorrowed", schemeName),
            schemeBorrowed.show
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.moneyborrowed.routes.WhySchemeBorrowedMoneyController.onSubmit(srn, index, mode).url
            ).withVisuallyHiddenContent("moneyBorrowedCheckYourAnswers.section.schemeBorrowed.hidden")
          )
        )
      )
    )

}
