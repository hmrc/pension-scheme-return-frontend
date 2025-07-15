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

package controllers.nonsipp.declaration

import pages.nonsipp.landorproperty._
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.SharesDisposalProgress
import navigation.Navigator
import utils.nonsipp._
import models.NormalMode
import play.api.i18n._
import models.requests.DataRequest
import pages.nonsipp.employercontributions.EmployerContributionsProgress
import services.{PsrRetrievalService, PsrVersionsService, SchemeDateService}
import viewmodels.models.SummaryPageViewModel.SummaryPageSection
import play.api.mvc._
import config.RefinedTypes.{Max300, Max50, Max5000}
import controllers.PSRController
import views.html.SummaryView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PreSubmissionSummaryController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: SummaryView,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport
    with SchemeDetailNavigationUtils {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    viewModel(srn).map(_.map(vm => Ok(view(vm, request.schemeDetails.schemeName))).merge)
  }

  def employerContributions(srn: Srn)(using
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, List[SummaryPageViewModel]] = {
    val indexes = request.userAnswers
      .map(EmployerContributionsProgress.all())
      .keys
      .map(refineStringIndex[Max300.Refined](_))
      .collect { case Some(i) => i }

    indexes.toList
      .map(EmployerContributionsCheckAnswersSectionUtils.employerContributionsSections(srn, _, NormalMode))
      .map(emp =>
        for {
          empList <- emp
          vm = CheckYourAnswersSummaryViewModel(
            heading = Message("nonsipp.summary.heading.employerContributions.memberTitle"),
            sections = empList
          )
        } yield List(SummaryPageSection(vm))
      )
      .reduce((a, b) =>
        for {
          aRight <- a
          bRight <- b
        } yield aRight ++ bRight
      )
  }

  def landOrProperties(
    srn: Srn
  )(using request: DataRequest[AnyContent], messages: Messages): Either[Result, List[CheckYourAnswersSection]] =
    val indexes = request.userAnswers
      .map(LandOrPropertyProgress.all())
      .filter(_._2.completed)
      .keys
      .map(refineStringIndex[Max5000.Refined](_))
      .collect { case Some(i) => i }

    indexes.toList
      .map(LandOrPropertyCheckAnswersSectionUtils.landOrPropertySections(srn, _, NormalMode))
      .reduce((a, b) =>
        for {
          aRight <- a
          bRight <- b
        } yield aRight ++ bRight
      )

  def sharesDisposals(srn: Srn)(using
    request: DataRequest[AnyContent],
    messages: Messages,
    ec: ExecutionContext
  ): Future[Either[Result, FormPageViewModel[CheckYourAnswersViewModel]]] = {
    val indexes: List[(Max5000, Max50)] = request.userAnswers
      .map(SharesDisposalProgress.all())
      .flatMap((shareIndex, disposals) => disposals.keys.map((shareIndex, _)))
      .toList
      .map((s, d) =>
        (
          refineStringIndex[Max5000.Refined](s),
          refineStringIndex[Max50.Refined](d)
        )
      )
      .collect { case (Some(s), Some(d)) => (s, d) }

    val futures = indexes
      .map((s, d) => SharesDisposalsCheckAnswersSectionUtils.sharesDisposalSections(srn, s, d, NormalMode))

    val future = Future.sequence(futures)

    future.map(
      _.reduce((a, b) =>
        for {
          aRight <- a
          bRight <- b
        } yield aRight.copy(page = aRight.page.copy(sections = aRight.page.sections ++ bRight.page.sections))
      ).map(_.copy(heading = Message("nonsipp.summary.heading.sharesDisposals")))
    )
  }

  def viewModel(
    srn: Srn
  )(using
    request: DataRequest[AnyContent],
    messages: Messages
  ): Future[Either[Result, FormPageViewModel[List[SummaryPageViewModel]]]] =
    sharesDisposals(srn).map { sharesDisposals =>
      for {
        basicDetailsSections <- BasicDetailsCheckAnswersSectionUtils.basicDetailsSections(
          srn,
          NormalMode,
          schemeDateService
        )
        employerContributionsSections <- employerContributions(srn)
        landOrPropertySections <- landOrProperties(srn)
        sharesDisposalsVm <- sharesDisposals
        allVms: List[SummaryPageViewModel] =
          List(
            SummaryPageSection(
              CheckYourAnswersSummaryViewModel(Message("nonsipp.summary.heading.basicDetails"), basicDetailsSections)
            )
          ) ++ employerContributionsSections
            ++ List(
              SummaryPageSection(
                CheckYourAnswersSummaryViewModel(
                  Message("nonsipp.summary.heading.landOrProperty"),
                  landOrPropertySections
                )
              ),
              SummaryPageSection(
                CheckYourAnswersSummaryViewModel(sharesDisposalsVm.heading, sharesDisposalsVm.page.sections)
              )
            )
      } yield FormPageViewModel[List[SummaryPageViewModel]](
        Message("nonsipp.summary.title"),
        Message("nonsipp.summary.heading"),
        allVms,
        routes.PsaDeclarationController.onPageLoad(srn)
      )
    }
}
