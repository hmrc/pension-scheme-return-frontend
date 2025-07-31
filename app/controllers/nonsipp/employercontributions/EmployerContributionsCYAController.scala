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

package controllers.nonsipp.employercontributions

import pages.nonsipp.memberdetails.MemberStatus
import play.api.mvc._
import utils.nonsipp.summary.EmployerContributionsCheckAnswersUtils
import utils.IntUtils.toRefined300
import cats.implicits.catsSyntaxApplicativeId
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import pages.nonsipp.employercontributions._
import services.{PsrSubmissionService, SaveService}
import config.RefinedTypes._
import controllers.PSRController
import cats.data.EitherT
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.{Inject, Named}

class EmployerContributionsCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Int, page: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max300, page: Int, mode: Mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    page: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max300, page: Int, mode: Mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max300, page: Int, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    EmployerContributionsCheckAnswersUtils
      .summaryData(srn, index, page, mode)
      .map { data =>
        Ok(view(EmployerContributionsCheckAnswersUtils.viewModel(data)))
      }
      .merge

  def onSubmit(srn: Srn, index: Int, page: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      // Set EmployerContributionsCompleted for each journey that's no longer In Progress:
      val userAnswersWithJourneysCompleted = EmployerContributionsCheckAnswersUtils
        .buildCompletedSecondaryIndexes(index)
        .map(
          _.foldLeft(Try(request.userAnswers))((userAnswers, secondaryIndex) =>
            userAnswers.set(EmployerContributionsCompleted(srn, index, secondaryIndex), SectionCompleted)
          )
        )

      (
        for {
          userAnswers <- EitherT(userAnswersWithJourneysCompleted.pure[Future])
          employerContributionsChanged = userAnswers.toOption.exists(
            _.changedList(_.buildEmployerContributions(srn, index))
          )
          updatedAnswers <- userAnswers
            .setWhen(employerContributionsChanged)(MemberStatus(srn, index), MemberState.Changed)
            .mapK[Future]
            .liftF
          _ <- saveService.save(updatedAnswers).liftF
          submissionResult <- psrSubmissionService
            .submitPsrDetailsWithUA(
              srn,
              updatedAnswers,
              fallbackCall = controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                .onPageLoad(srn, index, page, mode)
            )
            .liftF
        } yield submissionResult.getOrRecoverJourney(_ =>
          Redirect(navigator.nextPage(EmployerContributionsCYAPage(srn), mode, updatedAnswers))
        )
      ).merge
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }

}
