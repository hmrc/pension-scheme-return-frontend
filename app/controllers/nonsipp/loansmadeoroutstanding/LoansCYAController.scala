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
import config.Refined.Max5000
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.LoansCYAController._
import controllers.PSRController
import models.ConditionalYesNo._
import models.SchemeId.Srn
import models.{Security, _}
import navigation.Navigator
import pages.nonsipp.common.IdentityTypePage
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

  def onPageLoad(
    srn: Srn,
    index: Max5000,
    checkOrChange: CheckOrChange,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          receivedLoanType <- requiredPage(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient, mode))
          recipientName <- List(
            request.userAnswers.get(IndividualRecipientNamePage(srn, index, mode)),
            request.userAnswers.get(CompanyRecipientNamePage(srn, index, mode)),
            request.userAnswers.get(PartnershipRecipientNamePage(srn, index, mode)),
            request.userAnswers.get(OtherRecipientDetailsPage(srn, index, mode)).map(_.name)
          ).flatten.headOption.getOrRecoverJourney
          recipientDetails = List(
            request.userAnswers
              .get(IndividualRecipientNinoPage(srn, index, mode))
              .flatMap(_.value.toOption.map(_.value)),
            request.userAnswers.get(CompanyRecipientCrnPage(srn, index, mode)).flatMap(_.value.toOption.map(_.value)),
            request.userAnswers
              .get(PartnershipRecipientUtrPage(srn, index, mode))
              .flatMap(_.value.toOption.map(_.value)),
            request.userAnswers.get(OtherRecipientDetailsPage(srn, index, mode)).map(_.description)
          ).flatten.headOption
          recipientReasonNoDetails = List(
            request.userAnswers
              .get(IndividualRecipientNinoPage(srn, index, mode))
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(CompanyRecipientCrnPage(srn, index, mode))
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(PartnershipRecipientUtrPage(srn, index, mode))
              .flatMap(_.value.swap.toOption.map(_.value))
          ).flatten.headOption
          connectedParty = if (request.userAnswers
              .get(IsIndividualRecipientConnectedPartyPage(srn, index, mode))
              .isEmpty) {
            Right(request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index, mode)).get)
          } else if (request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index, mode)).get) {
            Left(request.userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, index, mode)).get)
          } else {
            Right(request.userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, index, mode)).get)
          }
          datePeriodLoan <- request.userAnswers.get(DatePeriodLoanPage(srn, index, mode)).getOrRecoverJourney
          loanAmount <- request.userAnswers.get(AmountOfTheLoanPage(srn, index, mode)).getOrRecoverJourney
          returnEndDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney.map(_.to)
          repaymentInstalments <- request.userAnswers
            .get(AreRepaymentsInstalmentsPage(srn, index, mode))
            .getOrRecoverJourney
          loanInterest <- request.userAnswers.get(InterestOnLoanPage(srn, index, mode)).getOrRecoverJourney
          outstandingArrearsOnLoan <- request.userAnswers
            .get(OutstandingArrearsOnLoanPage(srn, index, mode))
            .map(_.value.toOption)
            .getOrRecoverJourney
          securityOnLoan <- request.userAnswers
            .get(SecurityGivenForLoanPage(srn, index, mode))
            .map(_.value.toOption)
            .getOrRecoverJourney
        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
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
                outstandingArrearsOnLoan,
                securityOnLoan,
                checkOrChange,
                mode
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Max5000, checkOrChange: CheckOrChange, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Redirect(navigator.nextPage(LoansCYAPage(srn, index, mode), mode, request.userAnswers))
    }
}

case class ViewModelParameters(
  srn: Srn,
  index: Max5000,
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
  checkOrChange: CheckOrChange,
  mode: Mode
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
          parameters.mode
        )
      ),
      refresh = None,
      buttonText = parameters.checkOrChange.fold(check = "site.saveAndContinue", change = "site.continue"),
      onSubmit =
        routes.LoansCYAController.onSubmit(parameters.srn, parameters.index, parameters.checkOrChange, parameters.mode)
    )

  private def sections(
    srn: Srn,
    index: Max5000,
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
    ) ++ loanPeriodSection(srn, index, recipientName, loanDate, assetsValue, loanPeriod, mode) ++
      loanAmountSection(srn, index, totalLoan, repayments, outstanding, returnEndDate, repaymentInstalments, mode) ++
      loanInterestSection(srn, index, interestPayable, interestRate, interestPayments, mode) ++
      loanSecuritySection(srn, index, securityOnLoan, mode) ++
      loanOutstandingSection(srn, index, outstandingArrearsOnLoan, mode)

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
      case IdentityType.Individual => routes.IndividualRecipientNameController.onPageLoad(srn, index, CheckMode).url
      case IdentityType.UKCompany => routes.CompanyRecipientNameController.onPageLoad(srn, index, CheckMode).url
      case IdentityType.UKPartnership => routes.PartnershipRecipientNameController.onPageLoad(srn, index, CheckMode).url
      case IdentityType.Other => routes.OtherRecipientDetailsController.onPageLoad(srn, index, CheckMode).url
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
            routes.IndividualRecipientNinoController.onPageLoad(srn, index, CheckMode).url,
            "loanCheckYourAnswers.section1.recipientDetails.nino.hidden",
            "loanCheckYourAnswers.section1.recipientDetails.noNinoReason.hidden"
          )
        case IdentityType.UKCompany =>
          (
            Message("loanCheckYourAnswers.section1.recipientDetails.crn", recipientName),
            routes.CompanyRecipientCrnController.onPageLoad(srn, index, CheckMode).url,
            "loanCheckYourAnswers.section1.recipientDetails.crn.hidden",
            "loanCheckYourAnswers.section1.recipientDetails.noCrnReason.hidden"
          )
        case IdentityType.UKPartnership =>
          (
            Message("loanCheckYourAnswers.section1.recipientDetails.utr", recipientName),
            routes.PartnershipRecipientUtrController.onPageLoad(srn, index, CheckMode).url,
            "loanCheckYourAnswers.section1.recipientDetails.utr.hidden",
            "loanCheckYourAnswers.section1.recipientDetails.noUtrReason.hidden"
          )
        case IdentityType.Other =>
          (
            Message("loanCheckYourAnswers.section1.recipientDetails.other", recipientName),
            routes.OtherRecipientDetailsController.onPageLoad(srn, index, CheckMode).url,
            "loanCheckYourAnswers.section1.recipientDetails.other.hidden",
            ""
          )
      }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = receivedLoanType match {
      case IdentityType.Individual =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noNinoReason", recipientName) ->
          routes.IndividualRecipientNinoController.onPageLoad(srn, index, CheckMode).url
      case IdentityType.UKCompany =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noCrnReason", recipientName) ->
          routes.CompanyRecipientCrnController.onPageLoad(srn, index, CheckMode).url
      case IdentityType.UKPartnership =>
        Message("loanCheckYourAnswers.section1.recipientDetails.noUtrReason", recipientName) ->
          routes.PartnershipRecipientUtrController.onPageLoad(srn, index, CheckMode).url
      case IdentityType.Other =>
        Message("loanCheckYourAnswers.section1.recipientDetails.other", recipientName) ->
          routes.OtherRecipientDetailsController.onPageLoad(srn, index, CheckMode).url
    }

    val (connectedPartyKey, connectedPartyValue, connectedPartyHiddenKey, connectedPartyUrl): (
      Message,
      String,
      String,
      String
    ) = connectedParty match {

      case Left(value) =>
        (
          if (value) {
            (
              Message("loanCheckYourAnswers.section1.isIndividualRecipient.yes", recipientName),
              "Yes",
              "",
              routes.IsIndividualRecipientConnectedPartyController.onPageLoad(srn, index, CheckMode).url
            )
          } else {
            (
              Message("loanCheckYourAnswers.section1.isIndividualRecipient.no", recipientName),
              "No",
              "",
              routes.IsIndividualRecipientConnectedPartyController.onPageLoad(srn, index, CheckMode).url
            )
          }
        )

      case Right(SponsoringOrConnectedParty.Sponsoring) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.sponsoring",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, CheckMode).url
        )
      case Right(SponsoringOrConnectedParty.ConnectedParty) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.connectedParty",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, CheckMode).url
        )
      case Right(SponsoringOrConnectedParty.Neither) =>
        (
          Message("loanCheckYourAnswers.section1.sponsoringOrConnectedParty", recipientName),
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.neither",
          "loanCheckYourAnswers.section1.sponsoringOrConnectedParty.hidden",
          routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad(srn, index, CheckMode).url
        )
    }

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section1.heading")),
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section1.whoReceivedLoan", receivedLoan)
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                controllers.nonsipp.common.routes.IdentityTypeController
          //                  .onPageLoad(srn, index, mode, IdentitySubject.LoanRecipient)
          //                  .url
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section1.whoReceivedLoan.hidden")
          //            ),
            .withChangeAction(
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, index, CheckMode, IdentitySubject.LoanRecipient)
                .url,
              hidden = "loanCheckYourAnswers.section1.whoReceivedLoan.hidden"
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section1.recipientName", recipientName)
          //            .withAction(
          //              SummaryAction("site.change", recipientNameUrl)
          //                .withVisuallyHiddenContent("loanCheckYourAnswers.section1.recipientName.hidden")
          //            )
            .withChangeAction(
              recipientNameUrl,
              hidden = "loanCheckYourAnswers.section1.recipientName.hidden"
            )
        ) :?+ recipientDetails.map(
          details =>
            CheckYourAnswersRowViewModel(recipientDetailsKey, details)
            //              .withAction(
            //                SummaryAction("site.change", recipientDetailsUrl)
            //                  .withVisuallyHiddenContent(recipientDetailsIdChangeHiddenKey)
            //              )
              .withChangeAction(
                recipientDetailsUrl,
                hidden = recipientDetailsIdChangeHiddenKey
              )
        ) :?+ recipientReasonNoDetails.map(
          reason =>
            CheckYourAnswersRowViewModel(recipientNoDetailsReasonKey, reason)
            //              .withAction(
            //                SummaryAction("site.change", recipientNoDetailsUrl)
            //                  .withVisuallyHiddenContent(recipientDetailsNoIdChangeHiddenKey)
            //              )
              .withChangeAction(
                recipientNoDetailsUrl,
                hidden = recipientDetailsNoIdChangeHiddenKey
              )
        ) :+ CheckYourAnswersRowViewModel(connectedPartyKey, connectedPartyValue)
        //          .withAction(
        //            SummaryAction("site.change", connectedPartyUrl)
        //              .withVisuallyHiddenContent(connectedPartyHiddenKey)
        //          )
          .withChangeAction(
            connectedPartyUrl,
            hidden = connectedPartyHiddenKey
          )
      )
    )
  }

  private def loanPeriodSection(
    srn: Srn,
    index: Max5000,
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
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.loanDate.hidden")
          //            ),
            .withChangeAction(
              routes.DatePeriodLoanController.onPageLoad(srn, index, CheckMode).url,
              hidden = "loanCheckYourAnswers.section2.loanDate.hidden"
            ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section2.assetsValue", recipientName),
            s"£${assetsValue.displayAs}"
          )
          //            .withAction(
          //            SummaryAction(
          //              "site.change",
          //              routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url + "#assetsValue"
          //            ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.assetsValue.hidden")
          //          ),
            .withChangeAction(
              routes.DatePeriodLoanController.onPageLoad(srn, index, CheckMode).url + "#assetsValue",
              hidden = "loanCheckYourAnswers.section2.assetsValue.hidden"
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section2.loanPeriod", loanPeriodMonths)
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                routes.DatePeriodLoanController.onPageLoad(srn, index, mode).url + "#loanPeriod"
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section2.loanPeriod.hidden")
          //            )
            .withChangeAction(
              routes.DatePeriodLoanController.onPageLoad(srn, index, CheckMode).url + "#loanPeriod",
              hidden = "loanCheckYourAnswers.section2.loanPeriod.hidden"
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
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.total.hidden")
          //            ),
            .withChangeAction(
              routes.AmountOfTheLoanController.onPageLoad(srn, index, CheckMode).url,
              hidden = "loanCheckYourAnswers.section3.loanAmount.total.hidden"
            ),
          CheckYourAnswersRowViewModel(
            "loanCheckYourAnswers.section3.loanAmount.repayments",
            s"£${repayments.displayAs}"
          )
          //            .withAction(
          //            SummaryAction(
          //              "site.change",
          //              routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url + "#repayments"
          //            ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.repayments.hidden")
          //          ),
            .withChangeAction(
              routes.AmountOfTheLoanController.onPageLoad(srn, index, CheckMode).url + "#repayments",
              hidden = "loanCheckYourAnswers.section3.loanAmount.repayments.hidden"
            ),
          CheckYourAnswersRowViewModel(
            Message("loanCheckYourAnswers.section3.loanAmount.outstanding", returnEndDate.show),
            s"£${outstanding.displayAs}"
          )
          //            .withAction(
          //            SummaryAction(
          //              "site.change",
          //              routes.AmountOfTheLoanController.onPageLoad(srn, index, mode).url + "#outstanding"
          //            ).withVisuallyHiddenContent(
          //              Message("loanCheckYourAnswers.section3.loanAmount.outstanding.hidden", returnEndDate.show)
          //            )
          //          ),
            .withChangeAction(
              routes.AmountOfTheLoanController.onPageLoad(srn, index, CheckMode).url + "#outstanding",
              hidden = Message("loanCheckYourAnswers.section3.loanAmount.outstanding.hidden", returnEndDate.show)
            ),
          CheckYourAnswersRowViewModel(
            "loanCheckYourAnswers.section3.loanAmount.instalments",
            repaymentsInstalmentsValue
          )
          //            .withAction(
          //            SummaryAction(
          //              "site.change",
          //              routes.AreRepaymentsInstalmentsController.onPageLoad(srn, index, mode).url
          //            ).withVisuallyHiddenContent("loanCheckYourAnswers.section3.loanAmount.instalments.hidden")
          //          )
            .withChangeAction(
              routes.AreRepaymentsInstalmentsController.onPageLoad(srn, index, CheckMode).url,
              hidden = "loanCheckYourAnswers.section3.loanAmount.instalments.hidden"
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
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.payable.hidden")
          //            ),
            .withChangeAction(
              routes.InterestOnLoanController.onPageLoad(srn, index, CheckMode).url,
              hidden = "loanCheckYourAnswers.section4.payable.hidden"
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.rate", s"${interestRate.displayAs}%")
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url + "#rate"
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.rate.hidden")
          //            ),
            .withChangeAction(
              routes.InterestOnLoanController.onPageLoad(srn, index, CheckMode).url + "#rate",
              hidden = "loanCheckYourAnswers.section4.rate.hidden"
            ),
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section4.payments", s"£${interestPayments.displayAs}")
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                routes.InterestOnLoanController.onPageLoad(srn, index, mode).url + "#payments"
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section4.payments.hidden")
          //            )
            .withChangeAction(
              routes.InterestOnLoanController.onPageLoad(srn, index, CheckMode).url + "#payments",
              hidden = "loanCheckYourAnswers.section4.payments.hidden"
            )
        )
      )
    )

  private def loanOutstandingSection(
    srn: Srn,
    index: Max5000,
    outstandingArrearsOnLoan: Option[Money],
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val outstandingMessage = if (outstandingArrearsOnLoan.isEmpty) "site.no" else "site.yes"
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("loanCheckYourAnswers.section6.heading")),
        List(
          CheckYourAnswersRowViewModel("loanCheckYourAnswers.section6.arrears", outstandingMessage)
            .withAction(
              SummaryAction(
                "site.change",
                routes.OutstandingArrearsOnLoanController.onPageLoad(srn, index, mode).url
              ).withVisuallyHiddenContent("loanCheckYourAnswers.section6.arrears.hidden")
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
          //            .withAction(
          //              SummaryAction(
          //                "site.change",
          //                routes.SecurityGivenForLoanController.onPageLoad(srn, index, mode).url
          //              ).withVisuallyHiddenContent("loanCheckYourAnswers.section5.security.hidden")
          //            )
            .withChangeAction(
              routes.SecurityGivenForLoanController.onPageLoad(srn, index, CheckMode).url,
              hidden = "loanCheckYourAnswers.section5.security.hidden"
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
}
