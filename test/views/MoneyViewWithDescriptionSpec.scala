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
import views.html.MoneyViewWithDescription
import org.scalacheck.Gen
import play.api.data
import models.Money
import forms.mappings.errors.MoneyFormErrors

class MoneyViewWithDescriptionSpec extends ViewSpec with ViewBehaviours with Mappings {

  runningApplication { implicit app =>
    val view = injected[MoneyViewWithDescription]

    val moneyMapping = money(MoneyFormErrors.default(requiredKey = "money.error.required"))
    val moneyForm: data.Form[Money] = data.Form("value" -> moneyMapping)

    val tripleMoneyForm = data.Form(
      mapping(
        "value.1" -> moneyMapping,
        "value.2" -> moneyMapping,
        "value.3" -> moneyMapping
      )(Tuple3.apply)(Tuple3.unapply)
    )

    val invalidMoneyForm = moneyForm.bind(Map("value" -> ""))

    val singleMoneyQuestion = formPageViewModelGen(using singleQuestionGen(moneyForm))
    val tripleMoneyQuestion = formPageViewModelGen(using tripleQuestionGen(tripleMoneyForm))
    val invalidSingleMoneyQuestion = formPageViewModelGen(using singleQuestionGen(invalidMoneyForm))
    val viewModelGen = Gen.oneOf(singleMoneyQuestion, tripleMoneyQuestion, invalidSingleMoneyQuestion)

    "MoneyViewWithDescription" - {

      act.like(renderTitle(viewModelGen)(view(moneyForm, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(moneyForm, _), _.heading))
      act.like(renderInputWithLabel(singleMoneyQuestion)("value", view(moneyForm, _), _.page.firstField.label))
      act.like(renderErrors(invalidSingleMoneyQuestion)(view(invalidMoneyForm, _), _ => "money.error.required"))
      act.like(renderForm(viewModelGen)(view(moneyForm, _), _.onSubmit))
      act.like(renderButtonText(viewModelGen)(view(moneyForm, _), _.buttonText))
    }
  }
}
