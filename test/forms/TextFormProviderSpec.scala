/*
 * Copyright 2023 HM Revenue & Customs
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

package forms

import forms.behaviours.FieldBehaviours
import org.scalacheck.Gen
import play.api.data.Form
import uk.gov.hmrc.domain.Nino

class TextFormProviderSpec extends FieldBehaviours {

  private val formProvider = new TextFormProvider()

  ".nino" - {

    val duplicates = Gen.listOfN(8, ninoGen).sample.value
    val validNinoGen = ninoGen.suchThat(n => !duplicates.contains(n)).map(_.nino)
    val invalidNinoGen = nonEmptyString.suchThat(!Nino.isValid(_))

    val ninoForm: Form[Nino] = formProvider.nino(
      "nino.error.required",
      "nino.error.invalid",
      duplicates,
      "nino.error.duplicate"
    )

    behave.like(fieldThatBindsValidData(ninoForm, "value", validNinoGen))
    behave.like(mandatoryField(ninoForm, "value", "nino.error.required"))
    behave.like(invalidField(ninoForm, "value", "nino.error.invalid", invalidNinoGen))
    behave.like(fieldRejectDuplicates(ninoForm, "value", "nino.error.duplicate", duplicates.map(_.nino)))
  }
}
