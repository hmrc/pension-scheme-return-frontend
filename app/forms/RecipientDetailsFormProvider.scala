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

import config.Constants.{maxTextAreaLength, textAreaRegex}
import models.RecipientDetails
import play.api.data.Form
import play.api.data.Forms._
import forms.mappings.Mappings

import javax.inject.Inject

class RecipientDetailsFormProvider @Inject()() extends Mappings {

  val nameMaxLength = 160
  val nameRegex = """^[a-zA-Z0-9 \-'".@/]+$"""

  val name = "name"
  val description = "description"

  def apply(
    nameRequired: String,
    nameInvalid: String,
    nameLength: String,
    descriptionInvalid: String,
    descriptionLength: String
  ): Form[RecipientDetails] =
    Form(
      mapping(
        name -> text(nameRequired).verifying(
          firstError(
            regexp(nameRegex, nameInvalid),
            maxLength(nameMaxLength, nameLength)
          )
        ),
        description -> optionalText().verifying(
          whenNotEmpty(
            regexp(textAreaRegex, descriptionInvalid),
            maxLength(maxTextAreaLength, descriptionLength)
          )
        )
      )(RecipientDetails.apply)(RecipientDetails.unapply)
    )
}
