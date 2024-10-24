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
import config.RefinedTypes.Max300
import models.SchemeId.Srn
import models.UserAnswers.implicits.UserAnswersTryOps
import pages.nonsipp.memberpensionpayments._
import models.{Money, UserAnswers}

import javax.inject.Inject

@Singleton()
class PensionAmountReceivedTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, index: Max300, userAnswers: UserAnswers): Option[Double] =
    userAnswers.get(TotalAmountPensionPaymentsPage(srn, index)).map(_.value)

  // Save member specific answers
  def transformFromEtmp(
    srn: Srn,
    index: Max300,
    pensionAmountReceived: Double
  ): List[UserAnswers.Compose] =
    List[UserAnswers.Compose](
      _.set(TotalAmountPensionPaymentsPage(srn, index), Money(pensionAmountReceived))
    )
}
