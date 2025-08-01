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

package controllers.nonsipp.membertransferout

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import viewmodels.implicits._
import play.api.mvc._
import utils.IntUtils.{toInt, toRefined300}
import cats.implicits.{toShow, toTraverseOps}
import controllers.actions._
import navigation.Navigator
import models.requests.DataRequest
import utils.ListUtils.ListOps
import models.PensionSchemeType.PensionSchemeType
import config.RefinedTypes._
import controllers.PSRController
import controllers.nonsipp.membertransferout.TransfersOutCYAController._
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import pages.nonsipp.membertransferout._
import play.api.i18n.MessagesApi
import viewmodels.Margin
import utils.FunctionKUtils._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class TransfersOutCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
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

  def onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    (
      for {
        memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
        secondaryIndexes <- request.userAnswers
          .get(TransfersOutCompletedPages(srn, index))
          .map(_.keys.toList.flatMap(refineStringIndex[Max5.Refined]))
          .getOrRecoverJourney
      } yield secondaryIndexes
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
        .map {
          case Nil => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case journeys =>
            Ok(
              view(
                viewModel(
                  srn,
                  memberDetails.fullName,
                  index,
                  journeys,
                  mode,
                  viewOnlyUpdated = false,
                  optYear = request.year,
                  optCurrentVersion = request.currentVersion,
                  optPreviousVersion = request.previousVersion
                )
              )
            )
        }
        .merge
    ).merge

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val transfersOutChanged: Boolean =
        request.userAnswers.changedList(_.buildTransfersOut(srn, index))

      for {
        updatedUserAnswers <- request.userAnswers
          .setWhen(transfersOutChanged)(MemberStatus(srn, index), MemberState.Changed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall =
            controllers.nonsipp.membertransferout.routes.TransfersOutCYAController.onPageLoad(srn, index, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ =>
        Redirect(
          navigator.nextPage(TransfersOutCYAPage(srn), mode, request.userAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}

object TransfersOutCYAController {
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

  case class TransfersOutCYA(
    secondaryIndex: Max5,
    schemeName: String,
    receiveType: PensionSchemeType,
    transferOut: LocalDate
  )
}
