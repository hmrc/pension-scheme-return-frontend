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

package viewmodels.govuk

import play.api.data.Field
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.fieldset.{Fieldset, Legend}
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.{RadioItem, Radios}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FieldType, ListRadiosRow, RadioListRow, YesNoViewModel}
import viewmodels.{ErrorMessageAwareness, LegendSize}
import views.components.Components.renderMessage

object radios extends RadiosFluency

trait RadiosFluency {

  object RadiosViewModel extends ErrorMessageAwareness with FieldsetFluency {

    def apply(
      field: Field,
      items: Seq[RadioItem],
      legend: Legend
    )(implicit messages: Messages): Radios =
      apply(
        field = field,
        items = items,
        fieldset = FieldsetViewModel(legend)
      )

    def radioList(
      field: Field,
      items: List[ListRadiosRow],
      legend: Option[Legend]
    )(implicit messages: Messages): Radios =
      apply(
        field = field,
        items = items.map(
          item =>
            RadioItem(
              id = Some(item.index.toString),
              content = HtmlContent(renderMessage(item.text)),
              value = Some(item.index.toString)
            )
        ),
        fieldset = FieldsetViewModel(legend)
      )

    def apply(
      field: Field,
      items: Seq[RadioItem],
      legend: Option[Legend]
    )(implicit messages: Messages): Radios =
      apply(
        field = field,
        items = items,
        fieldset = FieldsetViewModel(legend)
      )

    def apply(
      field: Field,
      items: Seq[RadioItem]
    )(implicit messages: Messages): Radios =
      Radios(
        name = field.name,
        items = items.map(item => item.copy(checked = field.value.isDefined && field.value == item.value)),
        errorMessage = errorMessage(field)
      )

    def apply(
      field: Field,
      items: Seq[RadioItem],
      fieldset: Fieldset
    )(implicit messages: Messages): Radios =
      Radios(
        fieldset = Some(fieldset),
        name = field.name,
        items = items.map(item => item.copy(checked = field.value.isDefined && field.value == item.value)),
        errorMessage = errorMessage(field)
      )

    def yesNo(
      field: Field,
      legend: Legend
    )(implicit messages: Messages): Radios =
      yesNo(
        field = field,
        fieldset = FieldsetViewModel(legend),
        None,
        None,
        None,
        None
      )

    def yesNo(
      field: Field,
      legend: Option[Legend],
      yes: Option[Html],
      no: Option[Html]
    )(implicit messages: Messages): Radios =
      yesNo(
        field = field,
        fieldset = FieldsetViewModel(legend),
        yes,
        no,
        None,
        None
      )

    def yesNo(
      field: Field,
      fieldset: Fieldset,
      yes: Option[Html] = None,
      no: Option[Html] = None,
      yesHint: Option[Html] = None,
      noHint: Option[Html] = None
    )(implicit messages: Messages): Radios = {

      val items = Seq(
        RadioItem(
          id = Some(field.id),
          value = Some("true"),
          content = yes.fold[Content](Text(messages("site.yes")))(msg => HtmlContent(msg)),
          hint = yesHint.map(html => hint.HintViewModel(html))
        ),
        RadioItem(
          id = Some(s"${field.id}-no"),
          value = Some("false"),
          content = no.fold[Content](Text(messages("site.no")))(msg => HtmlContent(msg)),
          hint = noHint.map(html => hint.HintViewModel(html))
        )
      )

      apply(
        field = field,
        fieldset = fieldset,
        items = items
      )
    }

    def conditionalYesNo(
      field: Field,
      fieldYes: Field,
      fieldNo: Field,
      yes: YesNoViewModel,
      no: YesNoViewModel,
      whenYes: (Message, FieldType) => Html,
      whenNo: (Message, FieldType) => Html,
      legend: Option[Message]
    )(implicit messages: Messages): Radios =
      Radios(
        fieldset = Some(
          FieldsetViewModel(
            legend.map(legend => LegendViewModel(legend.toMessage).withSize(LegendSize.Medium))
          )
        ),
        name = field.name,
        items = Seq(
          RadioItem(
            id = Some("value_yes"),
            value = Some("true"),
            content = yes.message.fold[Content](Text(messages("site.yes")))(msg => HtmlContent(msg)),
            checked = fieldYes.errors.nonEmpty || (field.value.contains("true") && fieldYes.value
              .exists(s => !s.isEmpty)),
            conditionalHtml = yes match {
              case YesNoViewModel.Conditional(_, _, conditionalMessage, fieldType) =>
                Some(whenYes(conditionalMessage, fieldType))
              case _ => None
            }
          ),
          RadioItem(
            id = Some("value_no"),
            value = Some("false"),
            content = no.message.fold[Content](Text(messages("site.no")))(msg => HtmlContent(msg)),
            checked = fieldNo.errors.nonEmpty || (field.value.contains("false") && fieldNo.value
              .exists(s => !s.isEmpty)),
            conditionalHtml = no match {
              case YesNoViewModel.Conditional(_, _, conditionalMessage, fieldType) =>
                Some(whenNo(conditionalMessage, fieldType))
              case _ => None
            }
          )
        ),
        errorMessage = errorMessage(field)
      )
  }

  implicit class FluentRadios(radios: Radios) {

    def withHint(hint: Hint): Radios =
      radios.copy(hint = Some(hint))

    def withHint(hint: Option[Hint]): Radios =
      radios.copy(hint = hint)

    def withFormGroupClasses(classes: String): Radios =
      radios.copy(formGroupClasses = classes)

    def withIdPrefix(prefix: String): Radios =
      radios.copy(idPrefix = Some(prefix))

    def withCssClass(newClass: String): Radios =
      radios.copy(classes = s"${radios.classes} $newClass")

    def withAttribute(attribute: (String, String)): Radios =
      radios.copy(attributes = radios.attributes + attribute)

    def inline(): Radios =
      radios.withCssClass("govuk-radios--inline")
  }
}
