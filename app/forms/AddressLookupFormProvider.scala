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
import forms.mappings.errors.InputFormErrors
import models.PostcodeLookup
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class AddressLookupFormProvider @Inject()() {

  def apply(
    postCodeFormErrors: InputFormErrors,
    filterFormErrors: InputFormErrors
  ): Form[PostcodeLookup] =
    Form(
      mapping(
        "postcode" -> Mappings.input(postCodeFormErrors),
        "filter" -> Mappings.optionalInput(filterFormErrors)
      )(PostcodeLookup(_, _))(a => Some((a.postcode, a.filter)))
    )
}
