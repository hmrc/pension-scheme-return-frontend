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

import config.FrontendAppConfig
import models.SchemeId.Srn
import prepop._
import models.backend.responses._
import models.UserAnswers

import scala.math.Ordering.Implicits.infixOrderingOps
import scala.util.Try

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class PrePopulationService @Inject() (
  landOrPropertyPrePopulationProcessor: LandOrPropertyPrePopulationProcessor,
  memberPrePopulationProcessor: MemberPrePopulationProcessor,
  sharesPrePopulationProcessor: SharesPrePopulationProcessor,
  loanPrePopulationProcessor: LoansPrePopulationProcessor,
  bondsPrePopulationProcessor: BondsPrePopulationProcessor,
  loanProgressPrePopulationProcessor: LoansProgressPrePopulationProcessor,
  otherAssetsPrePopulationProcessor: OtherAssetsPrePopulationProcessor,
  sharesProgressPrePopulationProcessor: SharesProgressPrePopulationProcessor,
  config: FrontendAppConfig
) {

  /**
   * this logic assumes gap years are allowed in pre-population
   */
  def findLastSubmittedPsrFbInPreviousYears(
    versionsForYears: Seq[PsrVersionsForYearsResponse],
    yearFrom: LocalDate
  ): Option[String] =
    Option
      .when(config.prePopulationEnabled)(
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
      )
      .flatten

  private def isSubmitted(psrVersionsResponse: PsrVersionsResponse): Boolean = (
    psrVersionsResponse.reportStatus == ReportStatus.SubmittedAndInProgress
      || psrVersionsResponse.reportStatus == ReportStatus.SubmittedAndSuccessfullyProcessed
  )

  def buildPrePopulatedUserAnswers(baseReturnUA: UserAnswers, userAnswers: UserAnswers)(srn: Srn): Try[UserAnswers] =
    if (config.prePopulationEnabled) {
      for {
        ua0 <- landOrPropertyPrePopulationProcessor.clean(baseReturnUA, userAnswers)(srn)
        ua1 <- memberPrePopulationProcessor.clean(baseReturnUA, ua0)(srn)
        ua2 <- sharesPrePopulationProcessor.clean(baseReturnUA, ua1)(srn)
        ua3 <- loanPrePopulationProcessor.clean(baseReturnUA, ua2)(srn)
        ua4 <- bondsPrePopulationProcessor.clean(baseReturnUA, ua3)(srn)
        ua5 <- loanProgressPrePopulationProcessor.clean(baseReturnUA, ua4)(srn)
        ua6 <- otherAssetsPrePopulationProcessor.clean(baseReturnUA, ua5)(srn)
        ua7 <- sharesProgressPrePopulationProcessor.clean(baseReturnUA, ua6)(srn)
      } yield ua7
    } else {
      Try(userAnswers)
    }
}
