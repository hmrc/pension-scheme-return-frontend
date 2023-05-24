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

package views

import forms.mappings.Mappings
import org.scalacheck.Gen
import play.api.data
import play.api.data.Forms.mapping
import play.api.test.FakeRequest
import views.html.IntView

class IntViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[IntView]

    val intMapping = int("int.error.required")
    val intForm = data.Form("value" -> intMapping)

    val tripleIntForm = data.Form(
      mapping(
        "value.1" -> intMapping,
        "value.2" -> intMapping,
        "value.3" -> intMapping
      )(Tuple3.apply)(Tuple3.unapply)
    )

    val invalidForm = intForm.bind(Map("value" -> ""))

    val singleIntViewModel = formPageViewModelGen(singleQuestionGen(intForm))
    val invalidViewModel = formPageViewModelGen(singleQuestionGen(invalidForm))
    val tripleIntViewModel = formPageViewModelGen(tripleQuestionGen(tripleIntForm))
    val viewModelGen = Gen.oneOf(singleIntViewModel, tripleIntViewModel, invalidViewModel)

    implicit val request = FakeRequest()

    "IntView" - {

      act.like(renderTitle(viewModelGen)(view(_), _.title.key))
      act.like(renderHeading(viewModelGen)(view(_), _.heading))
      act.like(renderInputWithLabel(singleIntViewModel)("value", view(_), _.heading))
      act.like(renderInputWithLabel(tripleIntViewModel)("value.1", view(_), _.page.fields.head.label))
      act.like(renderInputWithLabel(tripleIntViewModel)("value.2", view(_), _.page.fields(1).label))
      act.like(renderInputWithLabel(tripleIntViewModel)("value.3", view(_), _.page.fields(2).label))
      act.like(renderErrors(invalidViewModel)(view(_), _ => "int.error.required"))
      act.like(renderForm(viewModelGen)(view(_), _.onSubmit))
      act.like(renderButtonText(viewModelGen)(view(_), _.buttonText))
    }
  }
}
