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

package utils.nonsipp

import services.{PsrRetrievalService, PsrVersionsService}
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import play.api.mvc.{AnyContent, Result}
import controllers.PSRController
import models.SchemeId.Srn
import cats.implicits.catsSyntaxApplicativeId
import pages.nonsipp.memberdetails.Paths.memberDetails
import models.requests.DataRequest
import pages.nonsipp.WhichTaxYearPage
import play.api.libs.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

trait SchemeDetailNavigationUtils { _: PSRController =>

  protected val psrVersionsService: PsrVersionsService
  protected val psrRetrievalService: PsrRetrievalService

  /**
   * This method determines whether the user has ability to proceed the byPassedJourney page.
   *
   * This is dependent on two factors: (1) the number of Active & Deferred members in the scheme, and (2) whether any
   * 'full' returns have been submitted for this scheme before.
   *
   * If the number of Active + Deferred members > 99, and no 'full' returns have been submitted for this scheme, then
   * the user is able to skip to the byPassedJourney page and returns true. In all other cases, returns false.
   *
   * A 'full' return must include at least 1 member, while a 'skipped' return will contain no member details at all, so
   * we use {{{.get(memberDetails).getOrElse(JsObject.empty).as[JsObject] == JsObject.empty}}} to determine whether or
   * not a retrieved set of `UserAnswers` refers to a 'full' or 'skipped' return.
   */
  def isJourneyBypassed(srn: Srn)(
    implicit request: DataRequest[AnyContent],
    ec: ExecutionContext
  ): Future[Either[Result, Boolean]] = {

    // Determine if the member threshold is reached
    val currentSchemeMembers = request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId))
    if (currentSchemeMembers.exists(_.totalActiveAndDeferred > 99)) {
      // If so, then determine if a full return was submitted this tax year
      val noFullReturnSubmittedThisTaxYear = request.previousUserAnswers match {
        case Some(previousUserAnswers) =>
          previousUserAnswers.get(memberDetails).getOrElse(JsObject.empty).as[JsObject] == JsObject.empty
        case None =>
          true
      }

      if (noFullReturnSubmittedThisTaxYear) {
        // If so, then determine if a full return was submitted last tax year
        request.userAnswers.get(WhichTaxYearPage(srn)) match {
          case Some(currentReturnTaxYear) =>
            val previousTaxYear = formatDateForApi(currentReturnTaxYear.from.minusYears(1))
            val previousTaxYearVersions = psrVersionsService.getVersions(request.schemeDetails.pstr, previousTaxYear)

            previousTaxYearVersions.map { psrVersionsResponses =>
              if (psrVersionsResponses.nonEmpty) {
                // If so, then determine if the latest submitted return from last year was a full return
                val latestVersionNumber = "%03d".format(psrVersionsResponses.map(_.reportVersion).max)
                val latestReturnFromPreviousTaxYear = psrRetrievalService.getAndTransformStandardPsrDetails(
                  optPeriodStartDate = Some(previousTaxYear),
                  optPsrVersion = Some(latestVersionNumber),
                  fallBackCall = controllers.routes.OverviewController.onPageLoad(srn)
                )

                latestReturnFromPreviousTaxYear.map { previousUserAnswers =>
                  Right(previousUserAnswers.get(memberDetails).getOrElse(JsObject.empty).as[JsObject] == JsObject.empty)
                }
              } else { // shortcut triggered: no returns of any kind submitted last year
                Right(true).pure[Future]
              }
            }.flatten
          case None => // Couldn't get current return's tax year, so something's gone wrong
            Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())).pure[Future]
        }
      } else { // No shortcut : full return was submitted earlier this tax year
        Right(false).pure[Future]
      }
    } else { // No shortcut : too few Active & Deferred members
      Right(false).pure[Future]
    }
  }
}
