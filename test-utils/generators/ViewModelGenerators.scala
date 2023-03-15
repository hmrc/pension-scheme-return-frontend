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

package generators

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import viewmodels.DisplayMessage.Message
import viewmodels.models._

trait ViewModelGenerators extends BasicGenerators {

  val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      title <- nonEmptyString
      heading <- nonEmptyString
      paragraphs <- Gen.listOf(nonEmptyString)
      buttonText <- nonEmptyString
      isStartButton <- boolean
      onSubmit <- call
    } yield {
      ContentPageViewModel(title, heading, paragraphs, buttonText, isStartButton, onSubmit)
    }

  val contentTablePageViewModelGen: Gen[ContentTablePageViewModel] =
    for {
      title <- nonEmptyString
      heading <- nonEmptyMessage
      inset <- nonEmptyMessage
      buttonText <- nonEmptyMessage
      rows <- Gen.listOf(tupleOf(nonEmptyMessage, nonEmptyMessage))
      onSubmit <- call
    } yield {
      ContentTablePageViewModel(title, heading, inset, buttonText, onSubmit, rows: _*)
    }

  val actionItemGen: Gen[SummaryAction] =
    for {
      content <- nonEmptyString.map(Message(_))
      href <- relativeUrl
      hidden <- nonEmptyString.map(Message(_))
    } yield {
      SummaryAction(content, href, hidden)
    }

  val summaryListRowGen: Gen[CheckYourAnswersRowViewModel] =
    for {
      key <- nonEmptyString
      value <- nonEmptyString
      items <- Gen.listOf(actionItemGen)
    } yield {
      CheckYourAnswersRowViewModel(Message(key), Message(value), items)
    }

  val checkYourAnswersViewModelGen: Gen[CheckYourAnswersViewModel] =
    for {
      title <- nonEmptyString
      heading <- nonEmptyString
      rows <- Gen.listOf(summaryListRowGen)
      onSubmit <- call
    } yield {
      CheckYourAnswersViewModel(Message(title), Message(heading), rows, onSubmit)
    }

  val bankAccountViewModelGen: Gen[BankAccountViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      paragraph <- nonEmptyMessage
      bankNameHeading <- nonEmptyMessage
      accountNumberHeading <- nonEmptyMessage
      accountNumberHint <- nonEmptyMessage
      sortCodeHeading <- nonEmptyMessage
      sortCodeHint <- nonEmptyMessage
      buttonText <- nonEmptyMessage
      onSubmit <- call
    } yield BankAccountViewModel(
      title,
      heading,
      paragraph,
      bankNameHeading,
      accountNumberHeading,
      accountNumberHint,
      sortCodeHeading,
      sortCodeHint,
      buttonText,
      onSubmit
    )

  val nameDOBViewModelGen: Gen[NameDOBViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      firstName <- nonEmptyMessage
      lastName <- nonEmptyMessage
      dateOfBirth <- nonEmptyMessage
      dateOfBirthHint <- nonEmptyMessage
      onSubmit <- call
    } yield NameDOBViewModel(
      title,
      heading,
      firstName,
      lastName,
      dateOfBirth,
      dateOfBirthHint,
      onSubmit
    )

  val summaryRowGen: Gen[ListRow] =
    for {
      text <- nonEmptyMessage
      changeUrl <- relativeUrl
      changeHiddenText <- nonEmptyMessage
      removeUrl <- relativeUrl
      removeHiddenText <- nonEmptyMessage
    } yield ListRow(text, changeUrl, changeHiddenText, removeUrl, removeHiddenText)

  def summaryViewModelGen(showRadios: Boolean = true): Gen[ListViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      rows <- Gen.choose(1, 10).flatMap(Gen.listOfN(_, summaryRowGen))
      radioText <- nonEmptyMessage
      insetText <- nonEmptyMessage
      onSubmit <- call
    } yield ListViewModel(
      title,
      heading,
      rows,
      radioText,
      insetText,
      showRadios,
      onSubmit
    )

  val pensionSchemeViewModelGen: Gen[PensionSchemeViewModel] =
    for {
      title <- nonEmptyString
      heading <- nonEmptyString
      onSubmit <- call
    } yield {
      PensionSchemeViewModel(title, heading, onSubmit)
    }

  val yesNoPageViewModelGen: Gen[YesNoPageViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      description <- Gen.listOf(nonEmptyMessage)
      legend <- Gen.option(nonEmptyMessage)
      onSubmit <- call
    } yield {
      YesNoPageViewModel(title, heading, description, legend, onSubmit)
    }

  def radioListRowViewModelGen: Gen[RadioListRowViewModel] =
    for {
      content <- nonEmptyMessage
      value <- nonEmptyString
    } yield {
      RadioListRowViewModel(content, value)
    }

  def radioListViewModelGen: Gen[RadioListViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      description <- Gen.listOf(nonEmptyMessage)
      listedContent <- Gen.listOf(nonEmptyMessage)
      legend <- Gen.option(nonEmptyMessage)
      items <- Gen.listOf(radioListRowViewModelGen)
      onSubmit <- call
    } yield {
      RadioListViewModel(title, heading, description, listedContent, legend, items, onSubmit)
    }

  val dateRangeViewModelGen: Gen[DateRangeViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      description <- Gen.option(nonEmptyMessage)
      startDateLabel <- nonEmptyMessage
      endDateLabel <- nonEmptyMessage
      onSubmit <- call
    } yield {
      DateRangeViewModel(
        title,
        heading,
        description,
        startDateLabel,
        endDateLabel,
        onSubmit
      )
    }

  val moneyViewModelGen: Gen[MoneyViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      onSubmit <- call
    } yield {
      MoneyViewModel(title, heading, onSubmit)
    }

  val textInputViewModelGen: Gen[TextInputViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      label <- Gen.option(nonEmptyMessage)
      onSubmit <- call
    } yield {
      TextInputViewModel(title, heading, label, onSubmit)
    }
}
