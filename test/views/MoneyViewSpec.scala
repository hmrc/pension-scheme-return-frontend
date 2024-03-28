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

import play.api.test.FakeRequest
import forms.mappings.Mappings
import play.api.data.Forms.mapping
import views.html.MoneyView
import org.scalacheck.Gen
import play.api.data
import models.Money
import forms.mappings.errors.MoneyFormErrors

class MoneyViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[MoneyView]

    implicit val request = FakeRequest()

    val moneyMapping = money(MoneyFormErrors.default(requiredKey = "money.error.required"))
    val moneyForm: data.Form[Money] =
      data.Form("value" -> moneyMapping)
    val tripleMoneyForm = data.Form(
      mapping(
        "value.1" -> moneyMapping,
        "value.2" -> moneyMapping,
        "value.3" -> moneyMapping
      )(Tuple3.apply)(Tuple3.unapply)
    )

    val invalidMoneyForm = moneyForm.bind(Map("value" -> ""))

    val singleMoneyQuestion = formPageViewModelGen(singleQuestionGen(moneyForm))
    val tripleMoneyQuestion = formPageViewModelGen(tripleQuestionGen(tripleMoneyForm))
    val invalidSingleMoneyQuestion = formPageViewModelGen(singleQuestionGen(invalidMoneyForm))
    val viewModelGen = Gen.oneOf(singleMoneyQuestion, tripleMoneyQuestion, invalidSingleMoneyQuestion)

    "MoneyView" - {

      act.like(renderTitle(viewModelGen)(view(_), _.title.key))
      act.like(renderHeading(viewModelGen)(view(_), _.heading))
      act.like(renderInputWithLabel(singleMoneyQuestion)("value", view(_), _.heading))
      act.like(renderInputWithLabel(tripleMoneyQuestion)("value.1", view(_), _.page.fields.head.label))
      act.like(renderInputWithLabel(tripleMoneyQuestion)("value.2", view(_), _.page.fields(1).label))
      act.like(renderInputWithLabel(tripleMoneyQuestion)("value.3", view(_), _.page.fields(2).label))
      act.like(renderErrors(invalidSingleMoneyQuestion)(view(_), _ => "money.error.required"))
      act.like(renderForm(viewModelGen)(view(_), _.onSubmit))
      act.like(renderButtonText(viewModelGen)(view(_), _.buttonText))
    }
  }
}
