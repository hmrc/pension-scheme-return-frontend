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
import cats.implicits.{toShow, toTraverseOps}
import uk.gov.hmrc.http.HeaderCarrier
import models.requests.DataRequest
import utils.ListUtils.ListOps
import models.PensionSchemeType.PensionSchemeType
import config.RefinedTypes._
import controllers.PsrControllerHelpers
import utils.DateTimeUtils.localDateShow
import models._
import pages.nonsipp.membertransferout._
import viewmodels.{DisplayMessage, Margin}
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

case class TransfersOutCYA(
  secondaryIndex: Max5,
  schemeName: String,
  receiveType: PensionSchemeType,
  transferOut: LocalDate
)

type TransfersOutData = (
  srn: Srn,
  memberName: String,
  index: Max300,
  journeys: List[TransfersOutCYA],
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

object TransfersOutCheckAnswersUtils extends CheckAnswersUtils[Max300, TransfersOutData] with PsrControllerHelpers {

  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(SchemeTransferOutPage(srn)).contains(true)

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(MemberTransferOutProgress.all())
    .filter((_, progressMap) => progressMap.exists((_, status) => status.completed))
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])
    .sortBy(i => request.userAnswers.get(MemberDetailsPage(srn, i)).map { case NameDOB(_, lastName, _) => lastName })

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.transfersOut.heading"))

  override def subheading(data: TransfersOutData): Option[DisplayMessage] = Some(
    Message("nonsipp.summary.transfersOut.subheading", data.memberName)
  )

  override def summaryDataAsync(srn: Srn, index: Max300, mode: Mode)(using
    DataRequest[AnyContent],
    HeaderCarrier,
    ExecutionContext
  ): Future[Either[Result, TransfersOutData]] = Future.successful(summaryData(srn, index, mode))

  def summaryData(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[Result, TransfersOutData] =
    for {
      memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
      secondaryIndexes <- request.userAnswers
        .get(TransfersOutCompletedPages(srn, index))
        .map(_.keys.toList.flatMap(refineStringIndex[Max5.Refined]))
        .getOrRecoverJourney
      journeys <- secondaryIndexes
        .traverse(journeyIndex =>
          for {
            schemeName <- request.userAnswers
              .get(ReceivingSchemeNamePage(srn, index, journeyIndex))
              .getOrRecoverJourney
            receiveType <- request.userAnswers
              .get(ReceivingSchemeTypePage(srn, index, journeyIndex))
              .getOrRecoverJourney
            transferOut <- request.userAnswers
              .get(WhenWasTransferMadePage(srn, index, journeyIndex))
              .getOrRecoverJourney

          } yield TransfersOutCYA(
            journeyIndex,
            schemeName,
            receiveType,
            transferOut
          )
        )
    } yield (
      srn,
      memberDetails.fullName,
      index,
      journeys,
      mode,
      false,
      request.year,
      request.currentVersion,
      request.previousVersion
    )

  override def viewModel(data: TransfersOutData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
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
    journeys: List[TransfersOutCYA],
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] = {
    val heading: InlineMessage =
      mode
        .fold(
          normal = "transfersOutCYAController.normal.heading",
          check = Message("transfersOutCYAController.heading.check", memberName),
          viewOnly = "transfersOutCYAController.viewOnly.heading"
        )

    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode
        .fold(
          normal = "transfersOutCYAController.normal.title",
          check = "transfersOutCYAController.title.check",
          viewOnly = "transfersOutCYAController.viewOnly.title"
        ),
      heading = heading,
      description = Some(ParagraphMessage("transfersOutCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        rows(srn, memberName, index, journeys),
        marginBottom = Some(Margin.Fixed60Bottom),
        inset = Option.when(journeys.size == 5)("transfersOutCYAController.inset")
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.continue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.membertransferout.routes.TransfersOutCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "transfersOutCYAController.viewOnly.title",
            heading = Message("transfersOutCYAController.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
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
    journeys: List[TransfersOutCYA]
  ): List[CheckYourAnswersSection] =
    journeys.zipWithIndex.map { case (journey, rowIndex) =>
      val (receiveTypeKey, receiveTypeValue) = journey.receiveType match {
        case PensionSchemeType.RegisteredPS(description) => ("pstr", description)
        case PensionSchemeType.QualifyingRecognisedOverseasPS(description) => ("qrops", description)
        case PensionSchemeType.Other(description) => ("other", description)
      }
      CheckYourAnswersSection(
        if (journeys.length == 1) None
        else
          Some(
            Heading2.medium(Message("transfersOutCYAController.section.heading", rowIndex + 1))
          ),
        List(
          CheckYourAnswersRowViewModel("transfersOutCYAController.rows.membersName", memberName),
          CheckYourAnswersRowViewModel("transfersOutCYAController.rows.receivingScheme", journey.schemeName)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                  .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                  .url
              ).withVisuallyHiddenContent("transfersOutCYAController.rows.receivingScheme")
            ),
          CheckYourAnswersRowViewModel(
            Message("transfersOutCYAController.rows.schemeType", journey.schemeName),
            s"transfersOutCYAController.rows.schemeRef.$receiveTypeKey.name"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membertransferout.routes.ReceivingSchemeTypeController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url + "#schemeType"
            ).withVisuallyHiddenContent(
              Message("transfersOutCYAController.rows.schemeType.hidden", journey.schemeName)
            )
          ),
          CheckYourAnswersRowViewModel(
            Message(s"transfersOutCYAController.rows.schemeRef.$receiveTypeKey", journey.schemeName),
            receiveTypeValue
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membertransferout.routes.ReceivingSchemeTypeController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url + "#schemeReference"
            ).withVisuallyHiddenContent(
              Message(s"transfersOutCYAController.rows.schemeRef.$receiveTypeKey.hidden", journey.schemeName)
            )
          ),
          CheckYourAnswersRowViewModel(
            Message("transfersOutCYAController.rows.dateOfTransfer", journey.schemeName, memberName),
            journey.transferOut.show
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membertransferout.routes.WhenWasTransferMadeController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url
            ).withVisuallyHiddenContent(
              Message("transfersOutCYAController.rows.dateOfTransfer.hidden", journey.schemeName, memberName)
            )
          )
        ) :?+ Option.when(rowIndex + 1 == journeys.length && rowIndex + 1 < 5)(
          CheckYourAnswersRowViewModel(
            Message("transfersOutCYAController.rows.reportAnotherTransfer", memberName),
            "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membertransferout.routes.ReportAnotherTransferOutController
                .onPageLoad(srn, index, journey.secondaryIndex, CheckMode)
                .url
            ).withVisuallyHiddenContent(
              Message("transfersOutCYAController.rows.reportAnotherTransfer.hidden", memberName)
            )
          )
        )
      )
    }

}
