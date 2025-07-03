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

import play.api.test.FakeRequest
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import controllers.TestValues
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import generators.ModelGenerators.allowedAccessRequestGen
import pages.nonsipp.declaration.PensionSchemeDeclarationPage
import models.requests.{AllowedAccessRequest, DataRequest}
import models.requests.psr.PsrDeclaration
import config.Constants.{PSA, PSP}

import scala.language.postfixOps

class DeclarationTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with BeforeAndAfterEach {

  private val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new DeclarationTransformer()
  private val pspDeclaration: PsrDeclaration = PsrDeclaration(
    submittedBy = PSP,
    submitterId = "submitterId",
    optAuthorisingPSAID = Some("authorisingPSAID"),
    declaration1 = true,
    declaration2 = true
  )
  private val psaDeclaration: PsrDeclaration = pspDeclaration.copy(
    submittedBy = PSA
  )

  "DeclarationTransformer" - {
    s"should transform to ETMP format" in {
      val request = DataRequest(allowedAccessRequest, emptyUserAnswers)
      val result = transformer.transformToEtmp(using request)
      result mustBe psaDeclaration.copy(
        submittedBy = if (request.pensionSchemeId.isPSP) PSP else PSA,
        submitterId = request.pensionSchemeId.value,
        optAuthorisingPSAID = request.schemeDetails.authorisingPSAID
      )
    }

    s"should transform from ETMP format" in {
      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, psaDeclaration)
      result.map(updatedUserAnswers => updatedUserAnswers.get(PensionSchemeDeclarationPage(srn)) mustBe defined)
    }
  }
}
