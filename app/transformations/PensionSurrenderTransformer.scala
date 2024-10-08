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

package transformations

import com.google.inject.Singleton
import config.Refined.Max300
import models.SchemeId.Srn
import pages.nonsipp.membersurrenderedbenefits._
import models.{Money, UserAnswers}
import viewmodels.models.SectionCompleted
import models.requests.psr._
import models.UserAnswers.implicits.UserAnswersTryOps

import javax.inject.Inject

@Singleton()
class PensionSurrenderTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, index: Max300, userAnswers: UserAnswers): Option[SurrenderedBenefits] =
    for {
      totalSurrendered <- userAnswers.get(SurrenderedBenefitsAmountPage(srn, index))
      dateOfSurrender <- userAnswers.get(WhenDidMemberSurrenderBenefitsPage(srn, index))
      surrenderReason <- userAnswers.get(WhyDidMemberSurrenderBenefitsPage(srn, index))
    } yield SurrenderedBenefits(
      totalSurrendered = totalSurrendered.value,
      dateOfSurrender = dateOfSurrender,
      surrenderReason = surrenderReason
    )

  // Save member specific answers
  def transformFromEtmp(
    srn: Srn,
    index: Max300,
    pensionSurrender: SurrenderedBenefits
  ): List[UserAnswers.Compose] =
    List[UserAnswers.Compose](
      _.set(SurrenderedBenefitsCompletedPage(srn, index), SectionCompleted),
      _.set(SurrenderedBenefitsAmountPage(srn, index), Money(pensionSurrender.totalSurrendered)),
      _.set(WhenDidMemberSurrenderBenefitsPage(srn, index), pensionSurrender.dateOfSurrender),
      _.set(WhyDidMemberSurrenderBenefitsPage(srn, index), pensionSurrender.surrenderReason)
    )
}
