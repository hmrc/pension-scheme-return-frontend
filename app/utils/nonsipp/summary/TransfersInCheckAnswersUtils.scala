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

import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import models.SchemeId.Srn
import utils.IntUtils.toInt
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{DisplayMessage, Margin}
import models.requests.DataRequest
import utils.ListUtils.ListOps
import models.PensionSchemeType.PensionSchemeType
import config.RefinedTypes._
import controllers.PsrControllerHelpers
import cats.implicits.{toShow, toTraverseOps}
import pages.nonsipp.receivetransfer._
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

type TransfersInData = (
  srn: Srn,
  memberName: String,
  index: Max300,
  journeys: List[TransfersInCYA],
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

case class TransfersInCYA(
  secondaryIndex: Max5,
  schemeName: String,
  transferType: PensionSchemeType,
  totalValue: Money,
  transferReceived: LocalDate,
  transferIncludeAsset: Boolean
)

object TransfersInCheckAnswersUtils extends CheckAnswersUtils[Max300, TransfersInData] with PsrControllerHelpers {

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.transfersIn.heading"))

  override def subheading(data: TransfersInData): Option[DisplayMessage] = Some(
    Message("nonsipp.summary.transfersIn.subheading", data.memberName)
  )

  override def summaryDataAsync(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Result, TransfersInData]] = Future.successful(summaryData(srn, index, mode))

  def summaryData(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[Result, TransfersInData] =
    for {
      memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
      secondaryIndexes <- request.userAnswers
        .get(TransfersInSectionCompletedForMember(srn, index))
        .map(_.keys.toList.flatMap(refineStringIndex[Max5.Refined]))
        .getOrRecoverJourney
      journeys <- secondaryIndexes
        .traverse(journeyIndex =>
          for {
            schemeName <- request.userAnswers
              .get(TransferringSchemeNamePage(srn, index, journeyIndex))
              .getOrRecoverJourney
            transferType <- request.userAnswers
              .get(TransferringSchemeTypePage(srn, index, journeyIndex))
              .getOrRecoverJourney
            totalValue <- request.userAnswers
              .get(TotalValueTransferPage(srn, index, journeyIndex))
              .getOrRecoverJourney
            transferReceived <- request.userAnswers
              .get(WhenWasTransferReceivedPage(srn, index, journeyIndex))
              .getOrRecoverJourney
            transferIncludeAsset <- request.userAnswers
              .get(DidTransferIncludeAssetPage(srn, index, journeyIndex))
              .getOrRecoverJourney
          } yield TransfersInCYA(
            journeyIndex,
            schemeName,
            transferType,
            totalValue,
            transferReceived,
            transferIncludeAsset
          )
        )
    } yield (
      srn,
      memberDetails.fullName,
      index,
      journeys,
      mode,
      false, // flag is not displayed on this tier
      request.year,
      request.currentVersion,
      request.previousVersion
    )

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(ReceiveTransferProgress.all())
    .filter { case (_, status) => status.exists(_._2.completed) }
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])

  override def viewModel(data: TransfersInData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.memberName,
    data.index,
    data.journeys,
    data.mode,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion
  )

  def viewModel(
    srn: Srn,
    memberName: String,
    index: Max300,
    journeys: List[TransfersInCYA],
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] = {
    val heading: InlineMessage =
      mode.fold(
        normal = "transfersInCYAController.normal.heading",
        check = Message("transfersInCYAController.heading.check", memberName),
        viewOnly = "transfersInCYAController.viewOnly.heading"
      )

    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode
        .fold(
          normal = "transfersInCYAController.normal.title",
          check = "transfersInCYAController.title.check",
          viewOnly = "transfersInCYAController.viewOnly.title"
        ),
      heading = heading,
      description = Some(ParagraphMessage("transfersInCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections = rows(srn, memberName, index, journeys),
        marginBottom = Some(Margin.Fixed60Bottom),
        inset = Option.when(journeys.size == 5)("transfersInCYAController.inset")
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.continue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.receivetransfer.routes.TransfersInCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "transfersInCYAController.viewOnly.title",
            heading = Message("transfersInCYAController.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )
  }

  private def rows(
    srn: Srn,
    memberName: String,
    index: Max300,
    journeys: List[TransfersInCYA]
  ): List[CheckYourAnswersSection] =
    journeys.zipWithIndex.map { case (journey, rowIndex) =>
      val (transferTypeKey, transferTypeValue) = journey.transferType match {
        case PensionSchemeType.RegisteredPS(description) => ("pstr", description)
        case PensionSchemeType.QualifyingRecognisedOverseasPS(description) => ("qrops", description)
        case PensionSchemeType.Other(description) => ("other", description)
      }
      CheckYourAnswersSection(
        if (journeys.length == 1) None
        else
          Some(
            Heading2.medium(Message("transfersInCYAController.section.heading", rowIndex + 1))
          ),
        List(
          CheckYourAnswersRowViewModel("transfersInCYAController.rows.membersName", memberName),
          CheckYourAnswersRowViewModel("transfersInCYAController.rows.transferringScheme", journey.schemeName)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
                  .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                  .url
              ).withVisuallyHiddenContent("transfersInCYAController.rows.transferringScheme")
            ),
          CheckYourAnswersRowViewModel(
            Message("transfersInCYAController.rows.schemeType", journey.schemeName),
            s"transfersInCYAController.rows.schemeRef.$transferTypeKey.name"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.receivetransfer.routes.TransferringSchemeTypeController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url + "#schemeType"
            ).withVisuallyHiddenContent(
              Message("transfersInCYAController.rows.schemeType.hidden", journey.schemeName)
            )
          ),
          CheckYourAnswersRowViewModel(
            Message(s"transfersInCYAController.rows.schemeRef.$transferTypeKey", journey.schemeName),
            transferTypeValue
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.receivetransfer.routes.TransferringSchemeTypeController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url + "#schemeReference"
            ).withVisuallyHiddenContent(
              Message(s"transfersInCYAController.rows.schemeRef.$transferTypeKey.hidden", journey.schemeName)
            )
          ),
          CheckYourAnswersRowViewModel(
            Message("transfersInCYAController.rows.totalValue", journey.schemeName, memberName),
            s"£${journey.totalValue.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.receivetransfer.routes.TotalValueTransferController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url
            ).withVisuallyHiddenContent(
              Message("transfersInCYAController.rows.totalValue.hidden", journey.schemeName, memberName)
            )
          ),
          CheckYourAnswersRowViewModel(
            Message("transfersInCYAController.rows.dateOfTransfer", journey.schemeName, memberName),
            journey.transferReceived.show
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.receivetransfer.routes.WhenWasTransferReceivedController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url
            ).withVisuallyHiddenContent(
              Message("transfersInCYAController.rows.dateOfTransfer.hidden", journey.schemeName, memberName)
            )
          ),
          CheckYourAnswersRowViewModel(
            Message("transfersInCYAController.rows.transferIncludeAsset", journey.schemeName, memberName),
            if (journey.transferIncludeAsset) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.receivetransfer.routes.DidTransferIncludeAssetController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url
            ).withVisuallyHiddenContent(
              Message("transfersInCYAController.rows.transferIncludeAsset.hidden", journey.schemeName, memberName)
            )
          )
        ) :?+ Option.when(rowIndex + 1 == journeys.length && rowIndex + 1 < 5)(
          CheckYourAnswersRowViewModel(
            Message("transfersInCYAController.rows.reportAnotherTransfer", memberName),
            "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.receivetransfer.routes.ReportAnotherTransferInController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url
            ).withVisuallyHiddenContent(
              Message("transfersInCYAController.rows.reportAnotherTransfer.hidden", memberName)
            )
          )
        )
      )
    }
}
