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

package controllers.nonsipp.loansmadeoroutstanding

import services.{PsrSubmissionService, SchemeDateService}
import viewmodels.implicits._
import models.ConditionalYesNo._
import play.api.mvc._
import utils.ListUtils.ListOps
import config.Refined.Max5000
import controllers.PSRController
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import play.api.i18n._
import controllers.nonsipp.loansmadeoroutstanding.LoansCYAController._
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{Security, _}
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.ExecutionContext

import java.time.LocalDate
import javax.inject.{Inject, Named}

class LoansCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  schemeDateService: SchemeDateService,
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
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient))
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient))
              .flatMap(_.value.swap.toOption.map(_.value))
          ).flatten.headOption
          connectedParty = if (request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index)).isEmpty) {
            Right(request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index)).get)
          } else {
            Left(request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index)).get)
          }
          datePeriodLoan <- request.userAnswers.get(DatePeriodLoanPage(srn, index)).getOrRecoverJourney
          loanAmount <- request.userAnswers.get(AmountOfTheLoanPage(srn, index)).getOrRecoverJourney
          returnEndDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney.map(_.to)
          repaymentInstalments <- request.userAnswers.get(AreRepaymentsInstalmentsPage(srn, index)).getOrRecoverJourney
          loanInterest <- request.userAnswers.get(InterestOnLoanPage(srn, index)).getOrRecoverJourney
          outstandingArrearsOnLoan <- request.userAnswers
            .get(OutstandingArrearsOnLoanPage(srn, index))
            .map(_.value.toOption)
            .getOrRecoverJourney
          securityOnLoan <- request.userAnswers
            .get(SecurityGivenForLoanPage(srn, index))
            .map(_.value.toOption)
            .getOrRecoverJourney

          schemeName = request.schemeDetails.schemeName
        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
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
                outstandingArrearsOnLoan,
                securityOnLoan,
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
          fallbackCall =
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, index, checkOrChange)
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) => Redirect(navigator.nextPage(LoansCYAPage(srn), NormalMode, request.userAnswers))
        }
    }
}

case class ViewModelParameters(
  srn: Srn,
  index: Max5000,
  schemeName: String,
  receivedLoanType: IdentityType,
  recipientName: String,
  recipientDetails: Option[String],
  recipientReasonNoDetails: Option[String],
  connectedParty: Either[Boolean, SponsoringOrConnectedParty],
  datePeriodLoan: (LocalDate, Money, Int),
  loanAmount: (Money, Money, Money),
  returnEndDate: LocalDate,
  repaymentInstalments: Boolean,
  loanInterest: (Money, Percentage, Money),
  outstandingArrearsOnLoan: Option[Money],
  securityOnLoan: Option[Security],
  checkOrChange: CheckOrChange
)
object LoansCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title =
        parameters.checkOrChange.fold(check = "checkYourAnswers.title", change = "loanCheckYourAnswers.change.title"),
      heading = parameters.checkOrChange.fold(
        check = "checkYourAnswers.heading",
        change =
          Message("loanCheckYourAnswers.change.heading", parameters.loanAmount._1.displayAs, parameters.recipientName)
      ),
      description = Some(ParagraphMessage("loansCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.index,
          parameters.schemeName,
          parameters.receivedLoanType,
          parameters.recipientName,
          parameters.recipientDetails,
          parameters.recipientReasonNoDetails,
          parameters.connectedParty,
          parameters.datePeriodLoan,
          parameters.loanAmount,
          parameters.returnEndDate,
          parameters.repaymentInstalments,
          parameters.loanInterest,
          parameters.outstandingArrearsOnLoan,
          parameters.securityOnLoan,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.checkOrChange.fold(check = "site.saveAndContinue", change = "site.continue"),
      onSubmit = routes.LoansCYAController.onSubmit(parameters.srn, parameters.index, parameters.checkOrChange)
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
    loanAmount: (Money, Money, Money),
    returnEndDate: LocalDate,
    repaymentInstalments: Boolean,
    loanInterest: (Money, Percentage, Money),
    outstandingArrearsOnLoan: Option[Money],
    securityOnLoan: Option[Security],
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
    ) ++ loanPeriodSection(srn, index, schemeName, loanDate, assetsValue, loanPeriod, mode) ++
      loanAmountSection(srn, index, totalLoan, repayments, outstanding, returnEndDate, repaymentInstalments, mode) ++
      loanInterestSection(srn, index, interestPayable, interestRate, interestPayments, mode) ++
      loanSecuritySection(srn, index, securityOnLoan, mode) ++
      loanOutstandingSection(srn, index, outstandingArrearsOnLoan, returnEndDate, mode)

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
      case IdentityType.Individual => routes.IndividualRecipientNameController.onPageLoad(srn, index, mode).url
      case IdentityType.UKCompany => routes.CompanyRecipientNameController.onPageLoad(srn, index, mode).url
      case IdentityType.UKPartnership => routes.PartnershipRecipientNameController.onPageLoad(srn, index, mode).url
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
            Message("loanCheckYourAnswers.section1.recipientDetails.nino", recipientName),
            routes.IndividualRecipientNinoController.onPageLoad(srn, index, mode).url,
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
        Message("loanCheckYourAnswers.section1.recipientDetails.noNinoReason", recipientName) ->
          routes.IndividualRecipientNinoController.onPageLoad(srn, index, mode).url
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
            Message("loanCheckYourAnswers.section1.isIndividualRecipient.yes", recipientName),
            "Yes",
            "",
            routes.IsIndividualRecipientConnectedPartyController.onPageLoad(srn, index, mode).url
          )
        } else {
          (
            Message("loanCheckYourAnswers.section1.isIndividualRecipient.no", recipientName),
            "No",
            "",
            routes.IsIndividualRecipientConnectedPartyController.onPageLoad(srn, index, mode).url
          )
        }

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
        ) :?+ recipientDetails.map(
          details =>
            CheckYourAnswersRowViewModel(recipientDetailsKey, details)
              .withAction(
                SummaryAction("site.change", recipientDetailsUrl)
                  .withVisuallyHiddenContent(recipientDetailsIdChangeHiddenKey)
              )
        ) :?+ recipientReasonNoDetails.map(
          reason =>
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
                routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.loanDate.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section2.assetsValue", schemeName),
            s"£${assetsValue.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url + "#assetsValue"
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.assetsValue.hidden")
          ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section2.loanPeriod", loanPeriodMonths)
            .withAction(
              SummaryAction(
                "site.change",
                routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url + "#loanPeriod"
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
              routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url + "#repayments"
            ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.repayments.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section3.loanAmount.outstanding", returnEndDate.show),
            s"£${outstanding.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url + "#outstanding"
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
    index: Max5000,
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
                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url + "#rate"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.rate.hidden")
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.payments", s"£${interestPayments.displayAs}")
            .withAction(
              SummaryAction(
                "site.change",
                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url + "#payments"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.payments.hidden")
            )
        )
      )
    )

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
                routes.SecurityGivenForLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section5.security.hidden")
            )
        ) :?+ securityOnLoan.map { value =>
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section5.security.yes", s"${value.security}")
            .withAction(
              SummaryAction(
                "site.change",
                routes.SecurityGivenForLoanController.onPageLoad(srn, index, mode).url + "#details"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section5.security.yes.hidden")
            )
        }
      )
    )
  }

  private def loanOutstandingSection(
    srn: Srn,
    index: Max5000,
    outstandingArrearsOnLoan: Option[Money],
    returnEndDate: LocalDate,
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val outstandingMessage = if (outstandingArrearsOnLoan.isEmpty) "site.no" else "site.yes"
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section6.heading")),
        List(
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section6.arrears", returnEndDate.show),
            outstandingMessage
          ).withAction(
            SummaryAction(
              "site.change",
              routes.OutstandingArrearsOnLoanController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent(("loanCheckYourAnswers.section6.arrears.hidden", returnEndDate.show))
          )
        ) :?+ outstandingArrearsOnLoan.map { value =>
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section6.arrears.yes", s"£${value.displayAs}")
            .withAction(
              SummaryAction(
                "site.change",
                routes.OutstandingArrearsOnLoanController.onPageLoad(srn, index, mode).url + "#details"
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section6.arrears.yes.hidden")
            )
        }
      )
    )
  }

}
