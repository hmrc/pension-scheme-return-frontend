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
import org.scalacheck.Gen._
import play.api.data.FormError
import utils.ListUtils._

class BankAccountFormProviderSpec extends FieldBehaviours {

  private val formProvider = new BankAccountFormProvider()

  import formProvider._

  val usedAccountNumbers = List(
    "12345678",
    "23456789",
    "34567890"
  )

  private val form = formProvider(
    "schemeBankDetails.bankName.error.required",
    "schemeBankDetails.bankName.error.invalid",
    "schemeBankDetails.bankName.error.length",
    "schemeBankDetails.accountNumber.error.required",
    "schemeBankDetails.accountNumber.error.invalid",
    "schemeBankDetails.accountNumber.error.length",
    "schemeBankDetails.accountNumber.error.duplicate",
    "schemeBankDetails.sortCode.error.required",
    "schemeBankDetails.sortCode.error.invalid",
    "schemeBankDetails.sortCode.error.format.invalid",
    "schemeBankDetails.sortCode.error.length",
    usedAccountNumbers
  )

  ".bankName" - {
    behave like fieldThatBindsValidData(form, "bankName", stringsWithMaxLength(bankNameMaxLength))
    behave like mandatoryField(form, "bankName", "schemeBankDetails.bankName.error.required")

    val lengthUpperLimit = 50
    val lengthFormError = FormError("bankName", "schemeBankDetails.bankName.error.length", List(bankNameMaxLength))
    behave like fieldLengthError(form, "bankName", lengthFormError, bankNameMaxLength + 1, lengthUpperLimit, alphaChar)

    behave like invalidAlphaField(
      form,
      fieldName = "bankName",
      errorMessage = "schemeBankDetails.bankName.error.invalid",
      args = List(bankNameRegex)
    )
  }

  ".accountNumber" - {
    behave like fieldThatBindsValidData(form, "accountNumber", numericStringLengthBetween(accountNumberMinLength, accountNumberMaxLength))
    behave like mandatoryField(form, "accountNumber", "schemeBankDetails.accountNumber.error.required")

    val lengthUpperLimit = 20
    val lengthFormError = FormError("accountNumber", "schemeBankDetails.accountNumber.error.length", List(accountNumberMinLength, accountNumberMaxLength))
    behave like fieldLengthError(form, "accountNumber", lengthFormError, 1, accountNumberMinLength - 1 , numChar)
    behave like fieldLengthError(form, "accountNumber", lengthFormError, accountNumberMaxLength + 1, lengthUpperLimit, numChar)

    behave like invalidNumericField(form, "accountNumber", "schemeBankDetails.accountNumber.error.invalid", accountNumberRegex)
    behave like fieldRejectDuplicates(form, "accountNumber", "schemeBankDetails.accountNumber.error.duplicate", usedAccountNumbers)
  }

  ".sortCode" - {
    "numerical format" - (behave like fieldThatBindsValidData(form, "sortCode", numericStringLength(sortCodeLength)))
    "standard format" - (behave like fieldThatBindsValidData(form, "sortCode", genSortCode()))

    behave like mandatoryField(form, "sortCode", "schemeBankDetails.sortCode.error.required")

    val lengthUpperLimit = 10
    val lengthFormError = FormError("sortCode", "schemeBankDetails.sortCode.error.length", List(sortCodeLength))
    behave like fieldLengthError(form, "sortCode", lengthFormError, 1, sortCodeLength - 1, numChar)
    behave like fieldLengthError(form, "sortCode", lengthFormError, sortCodeLength + 1, lengthUpperLimit, numChar)
    behave like invalidNumericField(form, "sortCode", "schemeBankDetails.sortCode.error.invalid", sortCodeValidRegex)
  }

  private def genSortCode(): Gen[String] = numericStringLength(sortCodeLength).map(_.toList.intersperse('-', 2).mkString)
}
