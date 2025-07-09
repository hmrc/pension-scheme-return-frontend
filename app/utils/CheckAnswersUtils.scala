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

package utils

import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import queries.Gettable
import play.api.mvc._
import config.RefinedTypes.Max3
import utils.nonsipp.TaskListStatusUtils.getBasicDetailsCompletedOrUpdated
import controllers.nonsipp.BasicDetailsCheckYourAnswersController
import models._
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.Messages
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel}
import models.requests.DataRequest
import play.api.mvc.Results.Redirect
import cats.data.NonEmptyList
import models.SchemeId.Srn
import pages.nonsipp.WhichTaxYearPage
import play.api.libs.json.Reads

import java.time.LocalDateTime

object CheckAnswersUtils {

  // Copy from PSRController
  private def requiredPage[A: Reads](page: Gettable[A])(implicit request: DataRequest[?]): Either[Result, A] =
    request.userAnswers.get(page) match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  // Copy from PSRController
  def loggedInUserNameOrRedirect(implicit request: DataRequest[?]): Either[Result, String] =
    request.minimalDetails.individualDetails match {
      case Some(individual) => Right(individual.fullName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  // Moved from BasicDetailsController
  def buildBasicDetailsViewModel(
    srn: Srn,
    mode: Mode,
    showBackLink: Boolean,
    eitherJourneyNavigationResultOrRecovery: Either[Result, Boolean],
    periods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    currentUserAnswers: UserAnswers,
    compilationOrSubmissionDate: Option[LocalDateTime] // added here as parameter
  )(implicit
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, FormPageViewModel[CheckYourAnswersViewModel]] =
    for {
      schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
      activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
      whyNoBankAccount = currentUserAnswers.get(WhyNoBankAccountPage(srn))
      whichTaxYearPage = currentUserAnswers.get(WhichTaxYearPage(srn))
      userName <- loggedInUserNameOrRedirect
      journeyByPassed <- eitherJourneyNavigationResultOrRecovery
    } yield BasicDetailsCheckYourAnswersController.viewModel(
      srn,
      mode,
      schemeMemberNumbers,
      activeBankAccount,
      whyNoBankAccount,
      whichTaxYearPage,
      periods,
      userName,
      request.schemeDetails,
      request.pensionSchemeId,
      request.pensionSchemeId.isPSP,
      viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
        getBasicDetailsCompletedOrUpdated(currentUserAnswers, request.previousUserAnswers.get) == Updated
      } else {
        false
      },
      optYear = request.year,
      optCurrentVersion = request.currentVersion,
      optPreviousVersion = request.previousVersion,
      compilationOrSubmissionDate = compilationOrSubmissionDate,
      journeyByPassed = journeyByPassed,
      showBackLink = showBackLink
    )
}
