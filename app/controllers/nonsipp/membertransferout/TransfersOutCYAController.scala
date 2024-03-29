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

package controllers.nonsipp.membertransferout

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined._
import cats.implicits.{toShow, toTraverseOps}
import controllers.actions._
import navigation.Navigator
import utils.ListUtils.ListOps
import models.PensionSchemeType.PensionSchemeType
import controllers.PSRController
import controllers.nonsipp.membertransferout.TransfersOutCYAController._
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import pages.nonsipp.membertransferout._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class TransfersOutCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
          secondaryIndexes <- request.userAnswers
            .get(TransfersOutCompletedPages(srn, index))
            .map(_.keys.toList.flatMap(refineStringIndex[Max5.Refined]))
            .getOrRecoverJourney
        } yield {
          secondaryIndexes
            .traverse(
              journeyIndex =>
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
              case journeys => Ok(view(viewModel(srn, memberDetails.fullName, index, journeys, mode)))
            }
            .merge
        }
      ).merge
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers
            .set(TransfersOutJourneyStatus(srn), SectionStatus.InProgress)
            .remove(TransferOutMemberListPage(srn))
        )
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedUserAnswers)
      } yield submissionResult.getOrRecoverJourney(
        _ =>
          Redirect(
            navigator.nextPage(TransfersOutCYAPage(srn), mode, request.userAnswers)
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
    mode: Mode
  ): FormPageViewModel[CheckYourAnswersViewModel] = {
    val heading: InlineMessage = mode.fold(
      normal = "checkYourAnswers.heading",
      check = Message("transfersOutCYAController.heading.check", memberName)
    )

    FormPageViewModel[CheckYourAnswersViewModel](
      title = "checkYourAnswers.title",
      heading = heading,
      description = Some(ParagraphMessage("transfersOutCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections = rows(srn, memberName, index, journeys),
        inset = Option.when(journeys.size == 5)("transfersOutCYAController.inset")
      ),
      refresh = None,
      buttonText = "site.continue",
      onSubmit = routes.TransfersOutCYAController.onSubmit(srn, index, mode)
    )
  }

  private def rows(
    srn: Srn,
    memberName: String,
    index: Max300,
    journeys: List[TransfersOutCYA]
  ): List[CheckYourAnswersSection] =
    journeys.zipWithIndex.map {
      case (journey, rowIndex) =>
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
