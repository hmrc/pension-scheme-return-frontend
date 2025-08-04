/*
 * Copyright 2025 HM Revenue & Customs
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

package utils.nonsipp.summary

import services.SchemeDateService
import viewmodels.implicits._
import models.ConditionalYesNo._
import play.api.mvc._
import utils.ListUtils.ListOps
import models.SchemeId.Srn
import utils.IntUtils.toInt
import cats.implicits.toShow
import uk.gov.hmrc.http.HeaderCarrier
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.DisplayMessage
import models.requests.DataRequest
import config.RefinedTypes.Max5000
import controllers.PsrControllerHelpers
import utils.DateTimeUtils.localDateShow
import models.{Security, _}
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

type LoansData = (
  srn: Srn,
  index: Max5000,
  schemeName: String,
  receivedLoanType: IdentityType,
  recipientName: String,
  recipientDetails: Option[String],
  recipientReasonNoDetails: Option[String],
  connectedParty: Either[Boolean, SponsoringOrConnectedParty],
  datePeriodLoan: (LocalDate, Money, Int),
  amountOfTheLoan: Either[String, AmountOfTheLoan],
  returnEndDate: LocalDate,
  repaymentInstalments: Boolean,
  interestOnLoan: Either[String, InterestOnLoan],
  arrearsPrevYears: Either[String, Boolean],
  outstandingArrearsOnLoan: Either[String, Option[Money]],
  securityOnLoan: Option[Security],
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

class LoansCheckAnswersUtils(schemeDateService: SchemeDateService)
    extends PsrControllerHelpers
    with CheckAnswersUtils[Max5000, LoansData] {

  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(LoansMadeOrOutstandingPage(srn)).contains(true)

  override def heading: Option[DisplayMessage] =
    Some(Message("nonsipp.summary.loans.heading"))

  override def subheading(data: LoansData): Option[DisplayMessage] =
    Some(Message("nonsipp.summary.loans.subheading", data.recipientName))

  def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max5000] =
    request.userAnswers
      .map(LoansProgress.all())
      .filter(_._2.completed)
      .keys
      .map(refineStringIndex[Max5000.Refined])
      .toList
      .flatten

  def summaryDataAsync(srn: Srn, index: Max5000, mode: Mode)(using
    request: DataRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[
    Result,
    LoansData
  ]] = Future.successful(summaryData(srn, index, mode))

  def summaryData(srn: Srn, index: Max5000, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[
    Result,
    LoansData
  ] =
    for {
      receivedLoanType <- requiredPage(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient))
      recipientName <- List(
        request.userAnswers.get(IndividualRecipientNamePage(srn, index)),
        request.userAnswers.get(CompanyRecipientNamePage(srn, index)),
        request.userAnswers.get(PartnershipRecipientNamePage(srn, index)),
        request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient)).map(_.name)
      ).flatten.headOption.getOrRecoverJourney
      recipientDetails = List(
        request.userAnswers.get(IndividualRecipientNinoPage(srn, index)).flatMap(_.value.toOption.map(_.value)),
        request.userAnswers
          .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient))
          .flatMap(_.value.toOption.map(_.value)),
        request.userAnswers
          .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient))
          .flatMap(_.value.toOption.map(_.value)),
        request.userAnswers
          .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient))
          .map(_.description)
      ).flatten.headOption
      recipientReasonNoDetails = List(
        request.userAnswers
          .get(IndividualRecipientNinoPage(srn, index))
          .flatMap(_.value.swap.toOption),
        request.userAnswers
          .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient))
          .flatMap(_.value.swap.toOption),
        request.userAnswers
          .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient))
          .flatMap(_.value.swap.toOption)
      ).flatten.headOption
      connectedParty =
        if (request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index)).isEmpty) {
          Right(request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index)).get)
        } else {
          Left(request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index)).get)
        }
      datePeriodLoan <- request.userAnswers.get(DatePeriodLoanPage(srn, index)).getOrRecoverJourney
      amountOfTheLoan = request.userAnswers.get(AmountOfTheLoanPage(srn, index)).getOrIncomplete
      returnEndDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney.map(_.to)
      repaymentInstalments <- request.userAnswers.get(AreRepaymentsInstalmentsPage(srn, index)).getOrRecoverJourney
      interestOnLoan = request.userAnswers.get(InterestOnLoanPage(srn, index)).getOrIncomplete
      arrearsPrevYears = request.userAnswers.get(ArrearsPrevYears(srn, index)).getOrIncomplete
      outstandingArrearsOnLoan = request.userAnswers
        .get(OutstandingArrearsOnLoanPage(srn, index))
        .map(_.value.toOption)
        .getOrIncomplete
      securityOnLoan <- request.userAnswers
        .get(SecurityGivenForLoanPage(srn, index))
        .map(_.value.toOption)
        .getOrRecoverJourney

      schemeName = request.schemeDetails.schemeName
    } yield (
      srn,
      index,
      schemeName,
      receivedLoanType,
      recipientName,
      recipientDetails,
      recipientReasonNoDetails,
      connectedParty,
      datePeriodLoan,
      amountOfTheLoan,
      returnEndDate,
      repaymentInstalments,
      interestOnLoan,
      arrearsPrevYears,
      outstandingArrearsOnLoan,
      securityOnLoan,
      mode,
      false,
      request.year,
      request.currentVersion,
      request.previousVersion
    )

  def viewModel(data: LoansData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.index,
    data.schemeName,
    data.receivedLoanType,
    data.recipientName,
    data.recipientDetails,
    data.recipientReasonNoDetails,
    data.connectedParty,
    data.datePeriodLoan,
    data.amountOfTheLoan,
    data.returnEndDate,
    data.repaymentInstalments,
    data.interestOnLoan,
    data.arrearsPrevYears,
    data.outstandingArrearsOnLoan,
    data.securityOnLoan,
    data.mode,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    receivedLoanType: IdentityType,
    recipientName: String,
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    connectedParty: Either[Boolean, SponsoringOrConnectedParty],
    datePeriodLoan: (LocalDate, Money, Int),
    amountOfTheLoan: Either[String, AmountOfTheLoan],
    returnEndDate: LocalDate,
    repaymentInstalments: Boolean,
    interestOnLoan: Either[String, InterestOnLoan],
    arrearsPrevYears: Either[String, Boolean],
    outstandingArrearsOnLoan: Either[String, Option[Money]],
    securityOnLoan: Option[Security],
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "loanCheckYourAnswers.normal.title",
        check = "loanCheckYourAnswers.change.title",
        viewOnly = "loanCheckYourAnswers.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "loanCheckYourAnswers.normal.heading",
        check = amountOfTheLoan match {
          case Right(loan) => Message("loanCheckYourAnswers.change.heading", loan.loanAmount.displayAs, recipientName)
          case Left(_) => Message("loansCheckAndUpdate.heading")
        },
        viewOnly = amountOfTheLoan match {
          case Right(loan) => Message("loanCheckYourAnswers.viewOnly.heading", loan.loanAmount.displayAs, recipientName)
          case Left(_) => Message("loansCheckAndUpdate.heading")
        }
      ),
      description = Some(ParagraphMessage("loansCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          index,
          schemeName,
          receivedLoanType,
          recipientName,
          recipientDetails,
          recipientReasonNoDetails,
          connectedParty,
          datePeriodLoan,
          amountOfTheLoan,
          returnEndDate,
          repaymentInstalments,
          interestOnLoan,
          arrearsPrevYears,
          outstandingArrearsOnLoan,
          securityOnLoan,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "loanCheckYourAnswers.viewOnly.title",
            amountOfTheLoan match {
              case Right(loan) =>
                Message("loanCheckYourAnswers.viewOnly.heading", loan.loanAmount.displayAs, recipientName)
              case Left(_) =>
                Message("loansCheckAndUpdate.heading")
            },
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onSubmit(srn, index, mode)
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
    receivedLoanType: IdentityType,
    recipientName: String,
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    connectedParty: Either[Boolean, SponsoringOrConnectedParty],
    datePeriodLoan: (LocalDate, Money, Int),
    amountOfTheLoan: Either[String, AmountOfTheLoan],
    returnEndDate: LocalDate,
    repaymentInstalments: Boolean,
    interestOnLoan: Either[String, InterestOnLoan],
    arrearsPrevYears: Either[String, Boolean],
    outstandingArrearsOnLoan: Either[String, Option[Money]],
    securityOnLoan: Option[Security],
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val (loanDate, assetsValue, loanPeriod) = datePeriodLoan
    val (totalLoan, repayments, outstanding) = amountOfTheLoan match {
      case Right(loan) => loan.asTuple
      case Left(_) => (Money.zero, None, None)
    }
    val repaymentsEither = repayments.toRight("Incomplete")
    val outstandingEither = outstanding.toRight("Incomplete")

    val (interestPayable, interestRate, interestPayments) = interestOnLoan match {
      case Right(interestOnLoan) => interestOnLoan.asTuple
      case Left(_) => (Money.zero, Percentage(0), None)
    }
    val interestPaymentsEither: Either[String, Option[Money]] = Right(interestPayments)

    recipientSection(
      srn,
      index,
      receivedLoanType,
      recipientName,
      recipientDetails,
      recipientReasonNoDetails,
      connectedParty,
      mode
    ) ++ loanPeriodSection(srn, index, schemeName, loanDate, assetsValue, loanPeriod, mode) ++
      loanAmountSection(
        srn,
        index,
        totalLoan,
        repaymentsEither,
        outstandingEither,
        returnEndDate,
        repaymentInstalments,
        mode
      ) ++
      loanInterestSection(
        srn,
        index,
        interestPayable,
        interestRate,
        interestPaymentsEither,
        mode
      ) ++
      loanSecuritySection(srn, index, securityOnLoan, mode) ++
      loanOutstandingSection(
        srn,
        index,
        arrearsPrevYears,
        outstandingArrearsOnLoan,
        returnEndDate,
        mode
      )

  }

  private def recipientSection(
    srn: Srn,
    index: Max5000,
    receivedLoanType: IdentityType,
    recipientName: String,
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    connectedParty: Either[Boolean, SponsoringOrConnectedParty],
    mode: Mode
  ): List[CheckYourAnswersSection] = {

    val receivedLoan = receivedLoanType match {
      case IdentityType.Individual => "loanRecipient.identityType.pageContent"
      case IdentityType.UKCompany => "loanRecipient.identityType.pageContent1"
      case IdentityType.UKPartnership => "loanRecipient.identityType.pageContent2"
      case IdentityType.Other => "loanRecipient.identityType.pageContent3"
    }

    val recipientNameUrl = receivedLoanType match {
      case IdentityType.Individual =>
        controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController
          .onPageLoad(srn, index, mode)
          .url
      case IdentityType.UKCompany =>
        controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientNameController
          .onPageLoad(srn, index, mode)
          .url
      case IdentityType.UKPartnership =>
        controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientNameController
          .onPageLoad(srn, index, mode)
          .url
      case IdentityType.Other =>
        controllers.nonsipp.common.routes.OtherRecipientDetailsController
          .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
          .url
    }

    val (
      recipientDetailsKey,
      recipientDetailsUrl,
      recipientDetailsIdChangeHiddenKey,
      recipientDetailsNoIdChangeHiddenKey
    ): (Message, String, String, String) =
      receivedLoanType match {
        case IdentityType.Individual =>
          (
            Message("loanCheckYourAnswers.section1.recipientDetails.nino"),
            controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNinoController
              .onPageLoad(srn, index, mode)
              .url,
            "loanCheckYourAnswers.section1.recipientDetails.nino.hidden",
            "loanCheckYourAnswers.section1.recipientDetails.noNinoReason.hidden"
          )
        case IdentityType.UKCompany =>
          (
            Message("loanCheckYourAnswers.section1.recipientDetails.crn", recipientName),
            controllers.nonsipp.common.routes.CompanyRecipientCrnController
              .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
              .url,
            "loanCheckYourAnswers.section1.recipientDetails.crn.hidden",
            "loanCheckYourAnswers.section1.recipientDetails.noCrnReason.hidden"
          )
        case IdentityType.UKPartnership =>
          (
            Message("loanCheckYourAnswers.section1.recipientDetails.utr", recipientName),
            controllers.nonsipp.common.routes.PartnershipRecipientUtrController
              .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
              .url,
            "loanCheckYourAnswers.section1.recipientDetails.utr.hidden",
            "loanCheckYourAnswers.section1.recipientDetails.noUtrReason.hidden"
          )
        case IdentityType.Other =>
          (
            Message("loanCheckYourAnswers.section1.recipientDetails.other", recipientName),
            controllers.nonsipp.common.routes.OtherRecipientDetailsController
              .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
              .url,
            "loanCheckYourAnswers.section1.recipientDetails.other.hidden",
            ""
          )
      }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = receivedLoanType match {
      case IdentityType.Individual =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noNinoReason") ->
          controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNinoController
            .onPageLoad(srn, index, mode)
            .url
      case IdentityType.UKCompany =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noCrnReason", recipientName) ->
          controllers.nonsipp.common.routes.CompanyRecipientCrnController
            .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
            .url
      case IdentityType.UKPartnership =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noUtrReason", recipientName) ->
          controllers.nonsipp.common.routes.PartnershipRecipientUtrController
            .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
            .url
      case IdentityType.Other =>
        Message("loanCheckYourAnswers.section1.recipientDetails.other", recipientName) ->
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
            .url
    }

    val (connectedPartyKey, connectedPartyValue, connectedPartyHiddenKey, connectedPartyUrl): (
      Message,
      String,
      String,
      String
    ) = connectedParty match {

      case Left(value) =>
        if (value) {
          (
            Message("loanCheckYourAnswers.section1.isIndividualRecipient.yes"),
            "Yes",
            "",
            controllers.nonsipp.loansmadeoroutstanding.routes.IsIndividualRecipientConnectedPartyController
              .onPageLoad(srn, index, mode)
              .url
          )
        } else {
          (
            Message("loanCheckYourAnswers.section1.isIndividualRecipient.no"),
            "No",
            "",
            controllers.nonsipp.loansmadeoroutstanding.routes.IsIndividualRecipientConnectedPartyController
              .onPageLoad(srn, index, mode)
              .url
          )
        }

      case Right(SponsoringOrConnectedParty.Sponsoring) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.sponsoring",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController
            .onPageLoad(srn, index, mode)
            .url
        )
      case Right(SponsoringOrConnectedParty.ConnectedParty) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.connectedParty",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController
            .onPageLoad(srn, index, mode)
            .url
        )
      case Right(SponsoringOrConnectedParty.Neither) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.neither",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController
            .onPageLoad(srn, index, mode)
            .url
        )
    }

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section1.heading")),
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section1.whoReceivedLoan", receivedLoan)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.common.routes.IdentityTypeController
                  .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
                  .url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section1.whoReceivedLoan.hidden")
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section1.recipientName", recipientName)
            .withAction(
              SummaryAction("site.change", recipientNameUrl)
                .withVisuallyHiddenContent("loanCheckYourAnswers.section1.recipientName.hidden")
            )
        ) :?+ recipientDetails.map(details =>
          CheckYourAnswersRowViewModel(recipientDetailsKey, details)
            .withAction(
              SummaryAction("site.change", recipientDetailsUrl)
                .withVisuallyHiddenContent(recipientDetailsIdChangeHiddenKey)
            )
        ) :?+ recipientReasonNoDetails.map(reason =>
          CheckYourAnswersRowViewModel(recipientNoDetailsReasonKey, reason)
            .withAction(
              SummaryAction("site.change", recipientNoDetailsUrl)
                .withVisuallyHiddenContent(recipientDetailsNoIdChangeHiddenKey)
            )
        ) :+ CheckYourAnswersRowViewModel(connectedPartyKey, connectedPartyValue)
          .withAction(
            SummaryAction("site.change", connectedPartyUrl)
              .withVisuallyHiddenContent(connectedPartyHiddenKey)
          )
      )
    )
  }

  private def loanPeriodSection(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    loanDate: LocalDate,
    assetsValue: Money,
    loanPeriod: Int,
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val loanPeriodMonths =
      if (loanPeriod == 1) {
        Message("date.month.value.lower", loanPeriod.toString)
      } else {
        Message("date.months.value.lower", loanPeriod.toString)
      }

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section2.heading")),
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section2.loanDate", loanDate.show)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.DatePeriodLoanController
                  .onPageLoad(srn, index, mode)
                  .url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.loanDate.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section2.assetsValue", schemeName),
            s"£${assetsValue.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.loansmadeoroutstanding.routes.DatePeriodLoanController
                .onPageLoad(srn, index, mode)
                .url + "#assetsValue"
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.assetsValue.hidden")
          ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section2.loanPeriod", loanPeriodMonths)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.DatePeriodLoanController
                  .onPageLoad(srn, index, mode)
                  .url + "#loanPeriod"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.loanPeriod.hidden")
            )
        )
      )
    )
  }

  private def loanAmountSection(
    srn: Srn,
    index: Max5000,
    totalLoan: Money,
    repayments: Either[String, Money],
    outstanding: Either[String, Money],
    returnEndDate: LocalDate,
    repaymentInstalments: Boolean,
    mode: Mode
  ): List[CheckYourAnswersSection] = {

    val repaymentsInstalmentsValue = if (repaymentInstalments) "site.yes" else "site.no"

    val optCapRepaymentCYString = repayments match {
      case Right(value) => s"£${value.displayAs}"
      case Left(value) => s"$value"
    }

    val optAmountOutstandingString = outstanding match {
      case Right(value) => s"£${value.displayAs}"
      case Left(value) => s"$value"
    }

    val isDataIncomplete = repayments.isLeft || outstanding.isLeft

    val header = if (isDataIncomplete) {
      Some(Heading2.medium("loansCheckAndUpdate.isPrePop.section3.heading"))
    } else {
      Some(Heading2.medium("loanCheckYourAnswers.section3.heading"))
    }

    List(
      CheckYourAnswersSection(
        header,
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section3.loanAmount.total", s"£${totalLoan.displayAs}")
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.AmountOfTheLoanController
                  .onPageLoad(srn, index, mode)
                  .url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.total.hidden")
            ),
          CheckYourAnswersRowViewModel(
            "loanCheckYourAnswers.section3.loanAmount.repayments",
            optCapRepaymentCYString
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.loansmadeoroutstanding.routes.AmountOfTheLoanController
                .onPageLoad(srn, index, mode)
                .url + "#repayments"
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.repayments.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section3.loanAmount.outstanding", returnEndDate.show),
            optAmountOutstandingString
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.loansmadeoroutstanding.routes.AmountOfTheLoanController
                .onPageLoad(srn, index, mode)
                .url + "#outstanding"
            ).withVisuallyHiddenContent(
              Message("loanCheckYourAnswers.section3.loanAmount.outstanding.hidden", returnEndDate.show)
            )
          ),
          CheckYourAnswersRowViewModel(
            "loanCheckYourAnswers.section3.loanAmount.instalments",
            repaymentsInstalmentsValue
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.loansmadeoroutstanding.routes.AreRepaymentsInstalmentsController
                .onPageLoad(srn, index, mode)
                .url
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.instalments.hidden")
          )
        )
      )
    )
  }

  private def loanInterestSection(
    srn: Srn,
    index: Max5000,
    interestPayable: Money,
    interestRate: Percentage,
    interestPayments: Either[String, Option[Money]],
    mode: Mode
  ): List[CheckYourAnswersSection] = {

    val isDataIncomplete = interestPayments.isLeft || interestPayments.exists(_.isEmpty)
    val header = if (isDataIncomplete) {
      Some(Heading2.medium("loansCheckAndUpdate.isPrePop.section4.heading"))
    } else {
      Some(Heading2.medium("loanCheckYourAnswers.section4.heading"))
    }

    val interestPaymentsDisplay: String = interestPayments match {
      case Left(_) => "Incomplete"
      case Right(Some(money)) => s"£${money.displayAs}"
      case Right(None) => "Incomplete"
    }

    List(
      CheckYourAnswersSection(
        header,
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.payable", s"£${interestPayable.displayAs}")
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.InterestOnLoanController
                  .onPageLoad(srn, index, mode)
                  .url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.payable.hidden")
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.rate", s"${interestRate.displayAs}%")
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.InterestOnLoanController
                  .onPageLoad(srn, index, mode)
                  .url + "#rate"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.rate.hidden")
            ),
          CheckYourAnswersRowViewModel(
            "loanCheckYourAnswers.section4.payments",
            interestPaymentsDisplay
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.loansmadeoroutstanding.routes.InterestOnLoanController
                .onPageLoad(srn, index, mode)
                .url + "#payments"
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.payments.hidden")
          )
        )
      )
    )
  }

  private def loanSecuritySection(
    srn: Srn,
    index: Max5000,
    securityOnLoan: Option[Security],
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val outstandingMessage = if (securityOnLoan.isEmpty) "site.no" else "site.yes"
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section5.heading")),
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section5.security", outstandingMessage)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.SecurityGivenForLoanController
                  .onPageLoad(srn, index, mode)
                  .url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section5.security.hidden")
            )
        ) :?+ securityOnLoan.map { value =>
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section5.security.yes", s"${value.security}")
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.SecurityGivenForLoanController
                  .onPageLoad(srn, index, mode)
                  .url + "#details"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section5.security.yes.hidden")
            )
        }
      )
    )
  }

  private def loanOutstandingSection(
    srn: Srn,
    index: Max5000,
    arrearsPrevYears: Either[String, Boolean],
    outstandingArrearsOnLoan: Either[String, Option[Money]],
    returnEndDate: LocalDate,
    mode: Mode
  ): List[CheckYourAnswersSection] = {

    val isDataIncomplete =
      arrearsPrevYears.isLeft ||
        (arrearsPrevYears == Right(true) && (outstandingArrearsOnLoan.isLeft || outstandingArrearsOnLoan.exists(
          _.isEmpty
        )))

    val header = if (isDataIncomplete) {
      Some(Heading2.medium("loansCheckAndUpdate.isPrePop.section6.heading"))
    } else {
      Some(Heading2.medium("loanCheckYourAnswers.section6.heading"))
    }

    val outstandingMessage = arrearsPrevYears match {
      case Right(true) => "site.yes"
      case Right(false) => "site.no"
      case Left(value) => s"$value"
    }

    val maybeOutstandingArrearsRow: Option[CheckYourAnswersRowViewModel] =
      (arrearsPrevYears, outstandingArrearsOnLoan) match {
        case (Right(true), Right(maybeMoney)) =>
          val valueText = maybeMoney match {
            case Some(value) => s"£${value.displayAs}"
            case None => "Incomplete"
          }

          Some(
            CheckYourAnswersRowViewModel(
              "loanCheckYourAnswers.section6.arrears.yes",
              valueText
            ).withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.loansmadeoroutstanding.routes.OutstandingArrearsOnLoanController
                  .onPageLoad(srn, index, mode)
                  .url + "#details"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section6.arrears.yes.hidden")
            )
          )

        case _ => None
      }

    List(
      CheckYourAnswersSection(
        header,
        List(
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section6.arrears", returnEndDate.show),
            outstandingMessage
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.loansmadeoroutstanding.routes.OutstandingArrearsOnLoanController
                .onPageLoad(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(
              Message("loanCheckYourAnswers.section6.arrears.hidden", returnEndDate.show)
            )
          )
        ) ++ maybeOutstandingArrearsRow.toList
      )
    )
  }
}

object LoansCheckAnswersUtils {
  def apply(schemeDateService: SchemeDateService): LoansCheckAnswersUtils = new LoansCheckAnswersUtils(
    schemeDateService
  )
}
