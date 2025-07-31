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

package controllers.nonsipp.membersurrenderedbenefits

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import play.api.mvc._
import utils.nonsipp.summary.MemberSurrenderedBenefitsCheckAnswersUtils
import utils.IntUtils.toRefined300
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import pages.nonsipp.membersurrenderedbenefits._
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SurrenderedBenefitsCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
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

  def onPageLoadCommon(srn: SchemeId.Srn, index: Max300, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    (
      for {
        memberDetails <- request.userAnswers
          .get(MemberDetailsPage(srn, index))
          .getOrRecoverJourney
        surrenderedBenefitsAmount <- request.userAnswers
          .get(SurrenderedBenefitsAmountPage(srn, index))
          .getOrRecoverJourney
        whenSurrenderedBenefits <- request.userAnswers
          .get(WhenDidMemberSurrenderBenefitsPage(srn, index))
          .getOrRecoverJourney
        whySurrenderedBenefits <- request.userAnswers
          .get(WhyDidMemberSurrenderBenefitsPage(srn, index))
          .getOrRecoverJourney
      } yield Ok(
        view(
          MemberSurrenderedBenefitsCheckAnswersUtils.viewModel(
            srn,
            index,
            memberDetails.fullName,
            surrenderedBenefitsAmount,
            whenSurrenderedBenefits,
            whySurrenderedBenefits,
            mode,
            viewOnlyUpdated = false,
            optYear = request.year,
            optCurrentVersion = request.currentVersion,
            optPreviousVersion = request.previousVersion
          )
        )
      )
    ).merge

  def onSubmit(srn: Srn, memberIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val surrenderedBenefitsChanged =
        request.userAnswers.changed(_.buildSurrenderedBenefits(srn, memberIndex))

      for {
        updatedUserAnswers <- request.userAnswers
          .set(SurrenderedBenefitsCompletedPage(srn, memberIndex), SectionCompleted)
          .setWhen(surrenderedBenefitsChanged)(MemberStatus(srn, memberIndex), MemberState.Changed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall = controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
            .onPageLoad(srn, memberIndex, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ =>
        Redirect(
          navigator.nextPage(SurrenderedBenefitsCYAPage(srn, memberIndex), mode, request.userAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}
