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

package controllers.nonsipp.loansmadeoroutstanding

import cats.implicits.toShow
import config.Refined.Max9999999
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.LoansCYAController._
import controllers.PSRController
import models.SchemeId.Srn
import models.{Money, Percentage, _}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding._
import play.api.i18n._
import play.api.mvc._
import services.SchemeDateService
import utils.DateTimeUtils.localDateShow
import utils.ListUtils.ListOps
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView
import utils.Tuple2Utils._

import java.time.LocalDate
import javax.inject.{Inject, Named}

class LoansCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  schemeDateService: SchemeDateService,
  view: CheckYourAnswersView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      (
        for {
          receivedLoanType <- requiredPage(WhoReceivedLoanPage(srn, index))
          recipientName <- List(
            request.userAnswers.get(IndividualRecipientNamePage(srn, index)),
            request.userAnswers.get(CompanyRecipientNamePage(srn, index)),
            request.userAnswers.get(PartnershipRecipientNamePage(srn, index)),
            request.userAnswers.get(OtherRecipientDetailsPage(srn, index)).map(_.name)
          ).flatten.headOption.getOrRecoverJourney
          recipientDetails = List(
            request.userAnswers.get(IndividualRecipientNinoPage(srn, index)).flatMap(_.value.toOption.map(_.value)),
            request.userAnswers.get(CompanyRecipientCrnPage(srn, index)).flatMap(_.value.toOption.map(_.value)),
            request.userAnswers.get(PartnershipRecipientUtrPage(srn, index)).flatMap(_.value.toOption.map(_.value)),
            request.userAnswers.get(OtherRecipientDetailsPage(srn, index)).map(_.description)
          ).flatten.headOption
          recipientReasonNoDetails = List(
            request.userAnswers
              .get(IndividualRecipientNinoPage(srn, index))
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers.get(CompanyRecipientCrnPage(srn, index)).flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers.get(PartnershipRecipientUtrPage(srn, index)).flatMap(_.value.swap.toOption.map(_.value))
          ).flatten.headOption
          connectedParty = (
            request.userAnswers.get(IsMemberOrConnectedPartyPage(srn, index)),
            request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index))
          ).toEither
          datePeriodLoan <- request.userAnswers.get(DatePeriodLoanPage(srn, index)).getOrRecoverJourney
          loanAmount <- request.userAnswers.get(AmountOfTheLoanPage(srn, index)).getOrRecoverJourney
          returnEndDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney.map(_.to)
          repaymentInstalments <- request.userAnswers.get(AreRepaymentsInstalmentsPage(srn, index)).getOrRecoverJourney
          loanInterest <- request.userAnswers.get(InterestOnLoanPage(srn, index)).getOrRecoverJourney
        } yield Ok(
          view(
            viewModel(
              srn,
              index,
              receivedLoanType,
              recipientName,
              recipientDetails,
              recipientReasonNoDetails,
              connectedParty,
              datePeriodLoan,
              loanAmount,
              returnEndDate,
              repaymentInstalments,
              loanInterest,
              mode
            )
          )
        )
      ).merge
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(LoansCYAPage(srn), mode, request.userAnswers))
  }
}

object LoansCYAController {
  def viewModel(
    srn: Srn,
    index: Max9999999,
    receivedLoanType: ReceivedLoanType,
    recipientName: String,
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    connectedParty: Either[MemberOrConnectedParty, SponsoringOrConnectedParty],
    datePeriodLoan: (LocalDate, Money, Int),
    loanAmount: (Money, Money, Money),
    returnEndDate: LocalDate,
    repaymentInstalments: Boolean,
    loanInterest: (Money, Percentage, Money),
    mode: Mode
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = "checkYourAnswers.title",
      heading = "checkYourAnswers.heading",
      description = Some(ParagraphMessage("loansCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          index,
          receivedLoanType,
          recipientName,
          recipientDetails,
          recipientReasonNoDetails,
          connectedParty,
          datePeriodLoan,
          loanAmount,
          returnEndDate,
          repaymentInstalments,
          loanInterest,
          mode
        )
      ),
      refresh = None,
      buttonText = "site.continue",
      onSubmit = routes.LoansCYAController.onSubmit(srn, mode)
    )

  private def sections(
    srn: Srn,
    index: Max9999999,
    receivedLoanType: ReceivedLoanType,
    recipientName: String,
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    connectedParty: Either[MemberOrConnectedParty, SponsoringOrConnectedParty],
    datePeriodLoan: (LocalDate, Money, Int),
    loanAmount: (Money, Money, Money),
    returnEndDate: LocalDate,
    repaymentInstalments: Boolean,
    loanInterest: (Money, Percentage, Money),
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val (loanDate, assetsValue, loanPeriod) = datePeriodLoan
    val (totalLoan, repayments, outstanding) = loanAmount
    val (interestPayable, interestRate, interestPayments) = loanInterest

    recipientSection(
      srn,
      index,
      receivedLoanType,
      recipientName,
      recipientDetails,
      recipientReasonNoDetails,
      connectedParty,
      mode
    ) ++ loanPeriodSection(srn, index, recipientName, loanDate, assetsValue, loanPeriod, mode) ++
      loanAmountSection(srn, index, totalLoan, repayments, outstanding, returnEndDate, repaymentInstalments, mode) ++
      loanInterestSection(srn, index, interestPayable, interestRate, interestPayments, mode)
  }

  private def recipientSection(
    srn: Srn,
    index: Max9999999,
    receivedLoanType: ReceivedLoanType,
    recipientName: String,
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    connectedParty: Either[MemberOrConnectedParty, SponsoringOrConnectedParty],
    mode: Mode
  ): List[CheckYourAnswersSection] = {

    val receivedLoan = receivedLoanType match {
      case ReceivedLoanType.Individual => "whoReceivedLoan.pageContent"
      case ReceivedLoanType.UKCompany => "whoReceivedLoan.pageContent1"
      case ReceivedLoanType.UKPartnership => "whoReceivedLoan.pageContent2"
      case ReceivedLoanType.Other => "whoReceivedLoan.pageContent3"
    }

    val recipientNameUrl = receivedLoanType match {
      case ReceivedLoanType.Individual => routes.IndividualRecipientNameController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.UKCompany => routes.CompanyRecipientNameController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.UKPartnership => routes.PartnershipRecipientNameController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.Other => routes.OtherRecipientDetailsController.onPageLoad(srn, index, mode).url
    }

    val (recipientDetailsKey, recipientDetailsUrl): (Message, String) = receivedLoanType match {
      case ReceivedLoanType.Individual =>
        Message("loanCheckYourAnswers.section1.recipientDetails.nino", recipientName) ->
          routes.IndividualRecipientNinoController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.UKCompany =>
        Message("loanCheckYourAnswers.section1.recipientDetails.crn", recipientName) ->
          routes.CompanyRecipientCrnController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.UKPartnership =>
        Message("loanCheckYourAnswers.section1.recipientDetails.utr", recipientName) ->
          routes.PartnershipRecipientUtrController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.Other =>
        Message("loanCheckYourAnswers.section1.recipientDetails.other", recipientName) ->
          routes.OtherRecipientDetailsController.onPageLoad(srn, index, mode).url
    }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = receivedLoanType match {
      case ReceivedLoanType.Individual =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noNinoReason", recipientName) ->
          routes.IndividualRecipientNinoController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.UKCompany =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noCrnReason", recipientName) ->
          routes.CompanyRecipientCrnController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.UKPartnership =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noUtrReason", recipientName) ->
          routes.PartnershipRecipientUtrController.onPageLoad(srn, index, mode).url
      case ReceivedLoanType.Other =>
        Message("loanCheckYourAnswers.section1.recipientDetails.other", recipientName) ->
          routes.OtherRecipientDetailsController.onPageLoad(srn, index, mode).url
    }

    val (connectedPartyKey, connectedPartyValue, connectedPartyHiddenKey, connectedPartyUrl): (
      Message,
      String,
      String,
      String
    ) = connectedParty match {
      case Left(MemberOrConnectedParty.Member) =>
        (
          Message("loanCheckYourAnswers.section1.memberOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.memberOrConnectedParty.member",
          "loanCheckYourAnswers.section1.memberOrConnectedParty.hidden",
          routes.IsMemberOrConnectedPartyController.onPageLoad(srn, index, mode).url
        )
      case Left(MemberOrConnectedParty.ConnectedParty) =>
        (
          Message("loanCheckYourAnswers.section1.memberOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.memberOrConnectedParty.connectedParty",
          "loanCheckYourAnswers.section1.memberOrConnectedParty.hidden",
          routes.IsMemberOrConnectedPartyController.onPageLoad(srn, index, mode).url
        )
      case Left(MemberOrConnectedParty.Neither) =>
        (
          Message("loanCheckYourAnswers.section1.memberOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.memberOrConnectedParty.neither",
          "loanCheckYourAnswers.section1.memberOrConnectedParty.hidden",
          routes.IsMemberOrConnectedPartyController.onPageLoad(srn, index, mode).url
        )
      case Right(SponsoringOrConnectedParty.Sponsoring) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.sponsoring",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, mode).url
        )
      case Right(SponsoringOrConnectedParty.ConnectedParty) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.connectedParty",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, mode).url
        )
      case Right(SponsoringOrConnectedParty.Neither) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.neither",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, mode).url
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
                routes.WhoReceivedLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section1.whoReceivedLoan.hidden")
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section1.recipientName", recipientName)
            .withAction(
              SummaryAction("site.change", recipientNameUrl)
                .withVisuallyHiddenContent("loanCheckYourAnswers.section1.recipientName.hidden")
            )
        ) :?+ recipientDetails.map(
          details =>
            CheckYourAnswersRowViewModel(recipientDetailsKey, details)
              .withAction(
                SummaryAction("site.change", recipientDetailsUrl)
                  .withVisuallyHiddenContent(s"$recipientDetailsKey.hidden")
              )
        ) :?+ recipientReasonNoDetails.map(
          reason =>
            CheckYourAnswersRowViewModel(recipientNoDetailsReasonKey, reason)
              .withAction(
                SummaryAction("site.change", recipientNoDetailsUrl)
                  .withVisuallyHiddenContent(s"$recipientNoDetailsReasonKey.hidden")
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
    index: Max9999999,
    recipientName: String,
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
                routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.loanDate.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section2.assetsValue", recipientName),
            s"£${assetsValue.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.assetsValue.hidden")
          ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section2.loanPeriod", loanPeriodMonths)
            .withAction(
              SummaryAction(
                "site.change",
                routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.loanPeriod.hidden")
            )
        )
      )
    )
  }

  private def loanAmountSection(
    srn: Srn,
    index: Max9999999,
    totalLoan: Money,
    repayments: Money,
    outstanding: Money,
    returnEndDate: LocalDate,
    repaymentInstalments: Boolean,
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val repaymentsInstalmentsValue = if (repaymentInstalments) "site.yes" else "site.no"

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section3.heading")),
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section3.loanAmount.total", s"£${totalLoan.displayAs}")
            .withAction(
              SummaryAction(
                "site.change",
                routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.total.hidden")
            ),
          CheckYourAnswersRowViewModel(
            "loanCheckYourAnswers.section3.loanAmount.repayments",
            s"£${repayments.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.repayments.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section3.loanAmount.outstanding", returnEndDate.show),
            s"£${outstanding.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url
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
              routes.AreRepaymentsInstalmentsController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.instalments.hidden")
          )
        )
      )
    )
  }

  private def loanInterestSection(
    srn: Srn,
    index: Max9999999,
    interestPayable: Money,
    interestRate: Percentage,
    interestPayments: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section4.heading")),
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.payable", s"£${interestPayable.displayAs}")
            .withAction(
              SummaryAction(
                "site.change",
                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.payable.hidden")
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.rate", s"${interestRate.displayAs}%")
            .withAction(
              SummaryAction(
                "site.change",
                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.rate.hidden")
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.payments", s"£${interestPayments.displayAs}")
            .withAction(
              SummaryAction(
                "site.change",
                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.payments.hidden")
            )
        )
      )
    )
}
