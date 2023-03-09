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
import play.api.data.Form
import uk.gov.hmrc.domain.Nino

import javax.inject.Inject

class TextFormProvider @Inject()() extends Mappings {

  def apply(requiredKey: String): Form[String] =
    Form(
      "value" -> text(requiredKey)
    )

  def nino(requiredKey: String, invalidKey: String, duplicates: List[Nino], duplicateKey: String): Form[Nino] =
    Form(
      "value" -> text(requiredKey)
        .verifying(invalidKey, Nino.isValid(_))
        .verifying(duplicateKey, !duplicates.map(_.nino).contains(_))
        .transform[Nino](Nino, _.nino)
    )
}