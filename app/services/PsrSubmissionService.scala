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

package services

import utils.nonsipp.TaskListCipUtils.transformTaskListToCipFormat
import pages.nonsipp.bonds.UnregulatedOrConnectedBondsHeldPage
import pages.nonsipp.otherassetsheld.OtherAssetsHeldPage
import connectors.PSRConnector
import models.SchemeId.Srn
import pages.nonsipp.landorproperty.LandOrPropertyHeldPage
import cats.implicits._
import transformations._
import pages.nonsipp.sharesdisposal.SharesDisposalPage
import uk.gov.hmrc.http.HeaderCarrier
import models.{DateRange, UserAnswers}
import pages.nonsipp.loansmadeoroutstanding._
import models.requests.DataRequest
import pages.nonsipp.otherassetsdisposal.OtherAssetsDisposalPage
import models.audit.PSRCompileAuditEvent
import pages.nonsipp.shares.DidSchemeHoldAnySharesPage
import play.api.mvc.Call
import models.requests.psr._
import config.Constants.{PSA, PSP}
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalPage
import pages.nonsipp.CheckReturnDatesPage
import utils.nonsipp.TaskListUtils.getSectionList
import play.api.libs.json.Json
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import pages.nonsipp.bondsdisposal.BondsDisposalPage

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class PsrSubmissionService @Inject()(
  psrConnector: PSRConnector,
  schemeDateService: SchemeDateService,
  auditService: AuditService,
  override val messagesApi: MessagesApi,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loanTransactionsTransformer: LoanTransactionsTransformer,
  landOrPropertyTransactionsTransformer: LandOrPropertyTransactionsTransformer,
  memberPaymentsTransformer: MemberPaymentsTransformer,
  moneyBorrowedTransformer: MoneyBorrowedTransformer,
  sharesTransformer: SharesTransformer,
  bondTransactionsTransformer: BondTransactionsTransformer,
  otherAssetTransactionsTransformer: OtherAssetTransactionsTransformer,
  declarationTransformer: DeclarationTransformer
) extends I18nSupport {

  def submitPsrDetailsWithUA(
    srn: Srn,
    userAnswers: UserAnswers,
    optFallbackCall: Option[Call] = None
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    submitPsrDetails(srn, optFallbackCall = optFallbackCall)(
      implicitly,
      implicitly,
      DataRequest(request.request, userAnswers)
    )

  def submitPsrDetails(
    srn: Srn,
    isSubmitted: Boolean = false,
    optFallbackCall: Option[Call] = None
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] = {

    val optSchemeHadLoans = request.userAnswers.get(LoansMadeOrOutstandingPage(srn))
    val optLandOrPropertyHeld = request.userAnswers.get(LandOrPropertyHeldPage(srn))
    val optMoneyWasBorrowed = request.userAnswers.get(MoneyBorrowedPage(srn))
    val optDisposeAnyLandOrProperty = request.userAnswers.get(LandOrPropertyDisposalPage(srn))
    val optDidSchemeHoldAnyShares = request.userAnswers.get(DidSchemeHoldAnySharesPage(srn))
    val optSharesDisposal = request.userAnswers.get(SharesDisposalPage(srn))
    val optUnregulatedOrConnectedBondsHeld = request.userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn))
    val optBondsDisposal = request.userAnswers.get(BondsDisposalPage(srn))
    val optOtherAssetsHeld = request.userAnswers.get(OtherAssetsHeldPage(srn))
    val optOtherAssetsDisposal = request.userAnswers.get(OtherAssetsDisposalPage(srn))

    (
      minimalRequiredSubmissionTransformer.transformToEtmp(srn),
      request.userAnswers.get(CheckReturnDatesPage(srn)),
      schemeDateService.schemeDate(srn)
    ).mapN { (minimalRequiredSubmission, checkReturnDates, taxYear) =>
      {
        // TODO fire audit event only if submission is successful
        val auditEvent = buildAuditEvent(taxYear, loggedInUserNameOrBlank(request))
        auditService.sendExtendedEvent(auditEvent)
        psrConnector.submitPsrDetails(
          PsrSubmission(
            minimalRequiredSubmission = minimalRequiredSubmission,
            checkReturnDates = checkReturnDates,
            loans = buildLoans(srn)(optSchemeHadLoans),
            assets = buildAssets(srn)(
              optLandOrPropertyHeld,
              optMoneyWasBorrowed,
              optDisposeAnyLandOrProperty,
              optUnregulatedOrConnectedBondsHeld,
              optBondsDisposal,
              optOtherAssetsHeld,
              optOtherAssetsDisposal
            ),
            membersPayments = memberPaymentsTransformer.transformToEtmp(srn, request.userAnswers),
            shares = buildShares(srn)(optDidSchemeHoldAnyShares, optSharesDisposal),
            psrDeclaration = Option.when(isSubmitted)(declarationTransformer.transformToEtmp)
          ),
          optFallbackCall
        )
      }
    }.sequence
  }

  private def loggedInUserNameOrBlank(implicit request: DataRequest[_]): String =
    request.minimalDetails.individualDetails match {
      case Some(individual) => individual.fullName
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => orgName
          case None => ""
        }
    }

  private def buildLoans(
    srn: Srn
  )(optSchemeHadLoans: Option[Boolean])(implicit request: DataRequest[_]): Option[Loans] =
    optSchemeHadLoans.map(
      schemeHadLoans => Loans(schemeHadLoans, loanTransactionsTransformer.transformToEtmp(srn))
    )

  private def buildAssets(srn: Srn)(
    optLandOrPropertyHeld: Option[Boolean],
    optMoneyWasBorrowed: Option[Boolean],
    optDisposeAnyLandOrProperty: Option[Boolean],
    optUnregulatedOrConnectedBondsHeld: Option[Boolean],
    optBondsDisposal: Option[Boolean],
    optOtherAssetsHeld: Option[Boolean],
    optOtherAssetsDisposal: Option[Boolean]
  )(implicit request: DataRequest[_]): Option[Assets] =
    Option.when(
      List(optLandOrPropertyHeld, optMoneyWasBorrowed, optUnregulatedOrConnectedBondsHeld, optOtherAssetsHeld).flatten.nonEmpty
    )(
      Assets(
        optLandOrProperty = optLandOrPropertyHeld.map(landOrPropertyHeld => {
          val disposeAnyLandOrProperty = optDisposeAnyLandOrProperty.getOrElse(false)
          LandOrProperty(
            landOrPropertyHeld = landOrPropertyHeld,
            disposeAnyLandOrProperty = disposeAnyLandOrProperty,
            landOrPropertyTransactions =
              landOrPropertyTransactionsTransformer.transformToEtmp(srn, disposeAnyLandOrProperty)
          )
        }),
        optBorrowing = optMoneyWasBorrowed.map(
          moneyWasBorrowed =>
            Borrowing(
              moneyWasBorrowed = moneyWasBorrowed,
              moneyBorrowed = moneyBorrowedTransformer.transformToEtmp(srn)
            )
        ),
        optBonds = optUnregulatedOrConnectedBondsHeld.map {
          val bondsDisposal = optBondsDisposal.getOrElse(false)
          bondsWereAdded =>
            Bonds(
              bondsWereAdded = bondsWereAdded,
              bondsWereDisposed = bondsDisposal,
              bondTransactions = bondTransactionsTransformer.transformToEtmp(srn, bondsDisposal)
            )
        },
        optOtherAssets = optOtherAssetsHeld.map {
          val otherAssetsDisposal = optOtherAssetsDisposal.getOrElse(false)
          otherAssetsHeld =>
            OtherAssets(
              otherAssetsWereHeld = otherAssetsHeld,
              otherAssetsWereDisposed = otherAssetsDisposal,
              otherAssetTransactions = otherAssetTransactionsTransformer.transformToEtmp(srn, otherAssetsDisposal)
            )
        }
      )
    )

  private def buildShares(srn: Srn)(
    optDidSchemeHoldAnyShares: Option[Boolean],
    optSharesDisposal: Option[Boolean]
  )(implicit request: DataRequest[_]): Option[Shares] =
    optDidSchemeHoldAnyShares.map { _ =>
      val sharesDisposal = optSharesDisposal.getOrElse(false)
      sharesTransformer.transformToEtmp(srn, sharesDisposal)
    }

  private def buildAuditEvent(taxYear: DateRange, userName: String)(
    implicit req: DataRequest[_]
  ) = PSRCompileAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    req.schemeDetails.establishers.headOption.fold(userName)(e => e.name),
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) PSP else PSA,
    taxYear = taxYear,
    taskList = Json
      .toJson(
        transformTaskListToCipFormat(
          getSectionList(req.srn, req.schemeDetails.schemeName, req.userAnswers, req.pensionSchemeId),
          messagesApi
        ).list
      )
  )
}
