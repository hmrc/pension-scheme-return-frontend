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

package controllers.nonsipp.receivetransfer

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import viewmodels.implicits._
import play.api.mvc._
import controllers.actions._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import controllers.nonsipp.receivetransfer.TransfersInCYAController._
import utils.ListUtils.ListOps
import models.PensionSchemeType.PensionSchemeType
import config.RefinedTypes._
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import cats.implicits.{toShow, toTraverseOps}
import pages.nonsipp.receivetransfer._
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models._
import utils.FunctionKUtils._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Named}

class TransfersInCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Max300,
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
          .get(TransfersInSectionCompletedForMember(srn, index))
          .map(_.keys.toList.flatMap(refineStringIndex[Max5.Refined]))
          .getOrRecoverJourney

      } yield {
        secondaryIndexes
          .traverse(
            journeyIndex =>
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
                    viewOnlyUpdated = false, // flag is not displayed on this tier
                    optYear = request.year,
                    optCurrentVersion = request.currentVersion,
                    optPreviousVersion = request.previousVersion,
                    compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
                  )
                )
              )
          }
          .merge
      }
    ).merge

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val transfersInChanged: Boolean =
        request.userAnswers.changedList(_.buildTransfersIn(srn, index))

      for {
        updatedUserAnswers <- request.userAnswers
          .setWhen(transfersInChanged)(MemberStatus(srn, index), MemberState.Changed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall =
            controllers.nonsipp.receivetransfer.routes.TransfersInCYAController.onPageLoad(srn, index, mode)
        )
      } yield submissionResult.getOrRecoverJourney(
        _ => Redirect(navigator.nextPage(TransfersInCYAPage(srn), mode, updatedUserAnswers))
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object TransfersInCYAController {
  def viewModel(
    srn: Srn,
    memberName: String,
    index: Max300,
    journeys: List[TransfersInCYA],
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] = {
    val heading: InlineMessage =
      mode.fold(
        normal = "checkYourAnswers.heading",
        check = Message("transfersInCYAController.heading.check", memberName),
        viewOnly = "transfersInCYAController.viewOnly.heading"
      )

    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode
        .fold(
          normal = "checkYourAnswers.title",
          check = "transfersInCYAController.title.check",
          viewOnly = "transfersInCYAController.viewOnly.title"
        ),
      heading = heading,
      description = Some(ParagraphMessage("transfersInCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections = rows(srn, memberName, index, journeys),
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
    journeys.zipWithIndex.map {
      case (journey, rowIndex) =>
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

  case class TransfersInCYA(
    secondaryIndex: Max5,
    schemeName: String,
    transferType: PensionSchemeType,
    totalValue: Money,
    transferReceived: LocalDate,
    transferIncludeAsset: Boolean
  )
}
