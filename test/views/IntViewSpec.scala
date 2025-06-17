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

package views

import forms.mappings.Mappings
import play.api.data.Forms.mapping
import views.html.IntView
import org.scalacheck.Gen
import play.api.data

class IntViewSpec extends ViewSpec with ViewBehaviours with Mappings {

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

    "IntView" - {

      act.like(renderTitle(viewModelGen)(view(intForm, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(intForm, _), _.heading))
      act.like(renderInputWithLabel(singleIntViewModel)("value", view(intForm, _), _.heading))
      act.like(renderInputWithLabel(tripleIntViewModel)("value.1", view(intForm, _), _.page.fields.head.label))
      act.like(renderInputWithLabel(tripleIntViewModel)("value.2", view(intForm, _), _.page.fields(1).label))
      act.like(renderInputWithLabel(tripleIntViewModel)("value.3", view(intForm, _), _.page.fields(2).label))
      act.like(renderErrors(invalidViewModel)(view(invalidForm, _), _ => "int.error.required"))
      act.like(renderForm(viewModelGen)(view(intForm, _), _.onSubmit))
      act.like(renderButtonText(viewModelGen)(view(intForm, _), _.buttonText))
    }
  }
}
