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

import forms.mappings.Mappings
import models.BankAccount
import play.api.data.Form
import play.api.data.Forms._
import mappings.implicits._

import javax.inject.Inject

class BankAccountFormProvider @Inject()() extends Mappings {

  val bankNameMaxLength = 28
  val bankNameRegex = "^[a-zA-Z\\-' ]+$"

  val accountNumberMinLength = 6
  val accountNumberMaxLength = 8
  val accountNumberRegex = "^[0-9]+$"

  val sortCodeValidRegex = "^[0-9- /]+$"
  val sortCodeLength = 6
  val sortCodeSimpleRegex = "^[0-9]{6}$"
  val sortCodeFullRegex = "^[0-9]{2}[- /]{1}[0-9]{2}[- /]{1}[0-9]{2}$"

  def apply(
    bankNameRequired: String,
    bankNameInvalid: String,
    bankNameWrongLength: String,
    accountNumberRequired: String,
    accountNumberInvalid: String,
    accountNumberWrongLength: String,
    accountNumberDuplicate: String,
    sortCodeRequired: String,
    sortCodeInvalid: String,
    sortCodeInvalidFormat: String,
    sortCodeWrongLength: String,
    usedAccountNumbers: List[String]
  ): Form[BankAccount] = Form(
    mapping(
      "bankName" -> text(bankNameRequired).verifying(
        firstError(
          regexp(bankNameRegex, bankNameInvalid),
          maxLength(bankNameMaxLength, bankNameWrongLength)
        )
      ),
      "accountNumber" -> text(accountNumberRequired).verifying(
        firstError(
          regexp(accountNumberRegex, accountNumberInvalid),
          lengthBetween(accountNumberMinLength, accountNumberMaxLength, accountNumberWrongLength),
          failWhen[String](usedAccountNumbers.contains, accountNumberDuplicate)
        )
      ),
      "sortCode" -> text(sortCodeRequired).verifying(
        firstError[String](
          regexp(sortCodeValidRegex, sortCodeInvalid),
          failWhen(_.count(_.isDigit) != sortCodeLength, sortCodeWrongLength, sortCodeLength),
          regexp(sortCodeSimpleRegex, sortCodeInvalidFormat).or(regexp(sortCodeFullRegex, sortCodeInvalidFormat))
        )
      )
    )(BankAccount.apply)(BankAccount.unapply)
  )
}
