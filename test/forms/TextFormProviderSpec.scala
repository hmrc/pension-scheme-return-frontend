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

package forms

import forms.behaviours.FieldBehaviours
import org.scalacheck.Gen._
import config.Constants.maxTextAreaLength
import org.scalacheck.Gen
import uk.gov.hmrc.domain.Nino
import play.api.data.Form

class TextFormProviderSpec extends FieldBehaviours {

  private val formProvider = new TextFormProvider()

  ".apply" - {
    val form: Form[String] = formProvider("required")

    behave.like(fieldThatBindsValidData(form, "value", nonEmptyAlphaString))
    behave.like(mandatoryField(form, "value", "required"))
    behave.like(trimmedField(form, "value", "     untrimmed value  "))
  }

  ".textarea" - {
    val form: Form[String] = formProvider.textArea(
      "required",
      "tooLong",
      "invalid"
    )

    val invalidTextGen = asciiPrintableStr.suchThat(_.trim.nonEmpty).suchThat(!_.matches(formProvider.textAreaRegex))

    behave.like(fieldThatBindsValidData(form, "value", nonEmptyAlphaString))
    behave.like(mandatoryField(form, "value", "required"))
    behave.like(invalidField(form, "value", "invalid", invalidTextGen))
    behave.like(textTooLongField(form, "value", "tooLong", maxTextAreaLength))
    behave.like(trimmedField(form, "value", "     untrimmed value  "))

    "allow punctuation" - {
      behave.like(
        fieldThatBindsValidData(
          form,
          "value",
          "Hi, I'm a test on date 10-12-2010 with email test@email.com \n with a newline and \ttab"
        )
      )
    }
  }

  ".nino" - {

    val duplicates = Gen.listOfN(8, ninoGen).sample.value
    val validNinoGen = ninoGen.suchThat(n => !duplicates.contains(n)).map(_.nino.toLowerCase)
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

  ".name" - {
    val form: Form[String] = formProvider.name(
      "required",
      "tooLong",
      "invalid"
    )

    val invalidTextGen = asciiPrintableStr.suchThat(_.trim.nonEmpty).suchThat(!_.matches(formProvider.nameRegex))

    behave.like(fieldThatBindsValidData(form, "value", nonEmptyAlphaString))
    behave.like(mandatoryField(form, "value", "required"))
    behave.like(invalidField(form, "value", "invalid", invalidTextGen))
    behave.like(textTooLongField(form, "value", "tooLong", maxTextAreaLength))
    behave.like(trimmedField(form, "value", "     untrimmed value  "))

    "specific test value example" - {
      behave.like(fieldThatBindsValidData(form, "value", "Aa Z'z-z"))
      behave.like(invalidField(form, "value", "invalid", "John324324"))
    }
  }
  ".psaId" - {
    val validId = "A" + numericStringLength(formProvider.psaIdMaxLength - 1).sample.value
    val invalidId = "B" + numericStringLength(formProvider.psaIdMaxLength - 1).sample.value
    val form: Form[String] = formProvider.psaId(
      "required",
      "tooLong",
      "invalid",
      "noMatch",
      Some(validId)
    )
    val formInvalid: Form[String] = formProvider.psaId(
      "required",
      "tooLong",
      "invalid",
      "noMatch",
      Some(invalidId)
    )

    behave.like(
      fieldThatBindsValidData(form, "value", validId)
    )
    behave.like(
      invalidField(formInvalid, "value", "invalid", invalidId)
    )
    behave.like(mandatoryField(formInvalid, "value", "required"))
    behave.like(trimmedField(form, "value", s"     $validId  "))
  }
}
