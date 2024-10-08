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
import pages.nonsipp.memberpensionpayments._
import models.{Money, UserAnswers}
import viewmodels.models.SectionStatus
import models.requests.psr.SectionDetails
import models.UserAnswers.implicits.UserAnswersTryOps

import javax.inject.Inject

@Singleton()
class PensionAmountReceivedTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, index: Max300, userAnswers: UserAnswers): Option[Double] =
    userAnswers.get(TotalAmountPensionPaymentsPage(srn, index)).map(_.value)

  // Build section details
  def transformToEtmp(srn: Srn, userAnswers: UserAnswers): SectionDetails = SectionDetails(
    made = userAnswers.get(PensionPaymentsReceivedPage(srn)).getOrElse(false),
    completed = false // TODO : CEM to be deleted
  )

  // Save member specific answers
  def transformFromEtmp(
    srn: Srn,
    index: Max300,
    pensionAmountReceived: Double
  ): List[UserAnswers.Compose] =
    List[UserAnswers.Compose](
      _.set(TotalAmountPensionPaymentsPage(srn, index), Money(pensionAmountReceived))
    )

  // Save section wide answers
  def transformFromEtmp(
    srn: Srn,
    pensionReceived: SectionDetails
  ): List[UserAnswers.Compose] = {
    val status: Option[SectionStatus] = pensionReceived match {
      case SectionDetails(made @ false, completed @ false) => None // not started
      case SectionDetails(made @ false, completed @ true) => Some(SectionStatus.Completed)
      case SectionDetails(made @ true, completed @ false) => Some(SectionStatus.Completed) // temporary E2E workaround
      case SectionDetails(made @ true, completed @ true) => Some(SectionStatus.Completed)
    }

    status.fold(List.empty[UserAnswers.Compose]) { s =>
      List[UserAnswers.Compose](
        _.setWhen(pensionReceived.started)(PensionPaymentsReceivedPage(srn), pensionReceived.made)
      )
    }
  }
}
