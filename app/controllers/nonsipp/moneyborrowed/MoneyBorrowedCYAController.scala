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
import utils.IntUtils.{toInt, toRefined5000}
import cats.implicits.toShow
import controllers.actions._
import models.requests.DataRequest
import controllers.nonsipp.moneyborrowed.MoneyBorrowedCYAController._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n._
import pages.nonsipp.moneyborrowed._
import viewmodels.DisplayMessage._
import viewmodels.models.SectionJourneyStatus.InProgress
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, index, mode)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers.get(MoneyBorrowedProgress(srn, index)) match {
      case Some(InProgress(_)) =>
        Redirect(
          controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, 1, mode)
        )
      case _ =>
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
                srn,
                index,
                schemeName,
                lenderName,
                lenderConnectedParty,
                borrowedAmountAndRate,
                whenBorrowed,
                schemeAssets,
                schemeBorrowed,
                mode,
                viewOnlyUpdated = false, // flag is not displayed on this tier
                optYear = request.year,
                optCurrentVersion = request.currentVersion,
                optPreviousVersion = request.previousVersion,
                compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
              )
            )
          )
        ).merge
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService
        .submitPsrDetails(
          srn,
          fallbackCall =
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, index, mode)
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) => Redirect(navigator.nextPage(MoneyBorrowedCYAPage(srn), NormalMode, request.userAnswers))
        }
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}

object MoneyBorrowedCYAController {
  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    lenderName: String,
    lenderConnectedParty: Boolean,
    borrowedAmountAndRate: (Money, Percentage),
    whenBorrowed: LocalDate,
    schemeAssets: Money,
    schemeBorrowed: String,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode
        .fold(
          normal = "moneyBorrowedCheckYourAnswers.title",
          check = "moneyBorrowedCheckYourAnswers.change.title",
          viewOnly = "moneyBorrowedCheckYourAnswers.viewOnly.title"
        ),
      heading = mode.fold(
        normal = "moneyBorrowedCheckYourAnswers.heading",
        check = Message(
          "moneyBorrowedCheckYourAnswers.change.heading",
          borrowedAmountAndRate._1.displayAs,
          lenderName
        ),
        viewOnly = "moneyBorrowedCheckYourAnswers.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          index,
          schemeName,
          lenderName,
          lenderConnectedParty,
          borrowedAmountAndRate,
          whenBorrowed,
          schemeAssets,
          schemeBorrowed,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode.isViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "moneyBorrowedCheckYourAnswers.viewOnly.title",
            heading =
              Message("moneyBorrowedCheckYourAnswers.viewOnly.heading", borrowedAmountAndRate._1.displayAs, lenderName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
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
                controllers.nonsipp.moneyborrowed.routes.LenderNameController.onSubmit(srn, index, mode).url
              ).withVisuallyHiddenContent("moneyBorrowedCheckYourAnswers.section.lenderName.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("moneyBorrowedCheckYourAnswers.section.lenderConnectedParty", lenderName.show),
            if (lenderConnectedParty) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.moneyborrowed.routes.IsLenderConnectedPartyController
                .onSubmit(srn, index, mode)
                .url + "#connected"
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
              controllers.nonsipp.moneyborrowed.routes.ValueOfSchemeAssetsWhenMoneyBorrowedController
                .onSubmit(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(
              ("moneyBorrowedCheckYourAnswers.section.schemeAssets.hidden", whenBorrowed.show)
            )
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
