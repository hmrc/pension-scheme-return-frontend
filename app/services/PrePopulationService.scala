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

package services

import models.backend.responses._

import scala.math.Ordering.Implicits.infixOrderingOps

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class PrePopulationService @Inject() {

  /**
   * this logic assumes gap years are allowed in pre-population
   * */
  def findLastSubmittedPsrFbInPreviousYears(
    versionsForYears: Seq[PsrVersionsForYearsResponse],
    yearFrom: LocalDate
  ): Option[String] =
    versionsForYears
      .filter(x => LocalDate.parse(x.startDate) < yearFrom)
      .sortBy(x => LocalDate.parse(x.startDate))(Ordering[LocalDate].reverse)
      .find(response => response.data.exists(isSubmitted))
      .flatMap { response =>
        response.data
          .filter(isSubmitted)
          .sortBy(_.reportVersion)(Ordering[Int].reverse)
          .headOption
          .map(_.reportFormBundleNumber)
      }

  private def isSubmitted(psrVersionsResponse: PsrVersionsResponse): Boolean = (
    psrVersionsResponse.reportStatus == ReportStatus.SubmittedAndInProgress
      || psrVersionsResponse.reportStatus == ReportStatus.SubmittedAndSuccessfullyProcessed
  )
}