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
import models.SchemeId.Srn
import models._
import pages.nonsipp.declaration.PensionSchemeDeclarationPage
import viewmodels.models.DeclarationViewModel
import models.requests.DataRequest
import models.requests.psr.PsrDeclaration
import config.Constants.{PSA, PSP}

import scala.util.Try

import javax.inject.Inject

@Singleton()
class DeclarationTransformer @Inject() {

  def transformToEtmp(implicit request: DataRequest[?]): PsrDeclaration =
    PsrDeclaration(
      submittedBy = if (request.pensionSchemeId.isPSP) PSP else PSA,
      submitterId = request.pensionSchemeId.value,
      optAuthorisingPSAID = request.schemeDetails.authorisingPSAID,
      declaration1 = true,
      declaration2 = true
    )

  def transformFromEtmp(
    userAnswers: UserAnswers,
    srn: Srn,
    psrDeclaration: PsrDeclaration
  ): Try[UserAnswers] =
    for {
      resultUA <- userAnswers.set(
        PensionSchemeDeclarationPage(srn),
        DeclarationViewModel(
          submitterType = psrDeclaration.submittedBy,
          submitterId = psrDeclaration.submitterId,
          optAuthorisingPSAID = psrDeclaration.optAuthorisingPSAID
        )
      )
    } yield resultUA
}
