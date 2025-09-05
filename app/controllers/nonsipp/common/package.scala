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

package controllers.nonsipp.common

import play.api.mvc.Call
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import controllers.nonsipp._
import models.{IdentitySubject, UserAnswers}

import scala.concurrent.Future

def saveProgress(
  srn: Srn,
  index: Max5000,
  userAnswers: UserAnswers,
  nextPage: Call,
  subject: IdentitySubject,
  alwaysCompleted: Boolean = false
): Future[UserAnswers] =
  subject match {
    case IdentitySubject.OtherAssetSeller =>
      otherassetsheld.saveProgress(srn, index, userAnswers, nextPage, alwaysCompleted)
    case IdentitySubject.LoanRecipient =>
      loansmadeoroutstanding.saveProgress(srn, index, userAnswers, nextPage, alwaysCompleted)
    case IdentitySubject.LandOrPropertySeller =>
      landorproperty.saveProgress(srn, index, userAnswers, nextPage, alwaysCompleted)
    case IdentitySubject.SharesSeller =>
      shares.saveProgress(srn, index, userAnswers, nextPage, alwaysCompleted)

    case _ => Future.successful(userAnswers)
  }
