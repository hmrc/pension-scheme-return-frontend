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

package controllers.nonsipp.memberdetails

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails._
import play.api.mvc._
import com.google.inject.Inject
import models.ManualOrUpload.Manual
import utils.nonsipp.summary.MemberDetailsCheckAnswersUtils
import utils.IntUtils.toRefined300
import cats.implicits.toTraverseOps
import controllers.actions._
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import uk.gov.hmrc.domain.Nino
import play.api.Logger
import navigation.Navigator
import utils.FunctionKUtils._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class SchemeMemberDetailsAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val logger = Logger(getClass)

  def onPageLoad(
    srn: Srn,
    index: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)
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

  def onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    request.userAnswers.get(MemberDetailsManualProgress(srn, index)) match {
      case Some(value) if value.inProgress =>
        Redirect(
          controllers.nonsipp.memberdetails.routes.SchemeMembersListController.onPageLoad(srn, 1, Manual)
        )
      case _ =>
        (
          for {
            memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index))
            hasNINO <- request.userAnswers.get(DoesMemberHaveNinoPage(srn, index))
            maybeNino <- Option.when(hasNINO)(request.userAnswers.get(MemberDetailsNinoPage(srn, index))).sequence
            maybeNoNinoReason <- Option.when(!hasNINO)(request.userAnswers.get(NoNINOPage(srn, index))).sequence
          } yield Ok(
            view(
              MemberDetailsCheckAnswersUtils.viewModel(
                index,
                srn,
                mode,
                memberDetails,
                hasNINO,
                maybeNino,
                maybeNoNinoReason,
                viewOnlyUpdated = false,
                optYear = request.year,
                optCurrentVersion = request.currentVersion,
                optPreviousVersion = request.previousVersion
              )
            )
          )
        ).getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val memberDetailsChanged: Boolean = (for {
        initial <- request.pureUserAnswers
        initialMemberPayments <- initial.buildMemberDetails(srn, index)
        currentMemberPayments <- request.userAnswers.buildMemberDetails(srn, index)
      } yield initialMemberPayments != currentMemberPayments).getOrElse(false)

      lazy val justAdded = mode.isNormalMode && request.userAnswers.get(MemberStatus(srn, index)).isEmpty

      lazy val addedThisVersion = request.userAnswers.get(MemberPsrVersionPage(srn, index)).isEmpty

      for {
        updatedUserAnswers <- request.userAnswers
          // Only set member state to CHANGED if something has actually changed
          // if memberPsrVersion is not present for member, do not set to CHANGED
          // CHANGED status is checked in the transformer later on to make sure this is the status we want to send to ETMP
          .setWhen(
            mode.isCheckMode && memberDetailsChanged && !addedThisVersion
          )(
            MemberStatus(srn, index), {
              logger.info(s"Something has changed in member payments for member index $index, setting state to CHANGED")
              MemberState.Changed
            }
          )
          // If member is already CHANGED, do not override with NEW if user goes back to CYA page on check mode
          .setWhen(justAdded)(MemberStatus(srn, index), MemberState.New)
          .setWhen(mode.isCheckMode && request.userAnswers.get(MemberDetailsCompletedPage(srn, index)).isEmpty)(
            MemberStatus(srn, index),
            MemberState.New
          )
          .setWhen(justAdded)(SafeToHardDelete(srn, index), Flag)
          .set(MemberDetailsCompletedPage(srn, index), SectionCompleted)
          .mapK[Future]
        nextPage = navigator.nextPage(SchemeMemberDetailsAnswersPage(srn), NormalMode, request.userAnswers)
        updatedProgressAnswers <- saveProgress(srn, index, updatedUserAnswers, nextPage, alwaysCompleted = true)
        _ <- saveService.save(updatedProgressAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedProgressAnswers,
          fallbackCall = controllers.nonsipp.memberdetails.routes.SchemeMemberDetailsAnswersController
            .onPageLoad(srn, index, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ => Redirect(nextPage))
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.memberdetails.routes.SchemeMembersListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object SchemeMemberDetailsAnswersController {}
