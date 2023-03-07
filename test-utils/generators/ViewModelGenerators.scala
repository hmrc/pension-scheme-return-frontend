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
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models._

trait ViewModelGenerators extends BasicGenerators {

  val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      title      <- nonEmptyString
      heading    <- nonEmptyString
      paragraphs <- Gen.listOf(nonEmptyString)
      buttonText <- nonEmptyString
      isStartButton <-boolean
      onSubmit   <- call
    } yield {
      ContentPageViewModel(title, heading, paragraphs, buttonText, isStartButton, onSubmit)
    }

  val contentTablePageViewModelGen: Gen[ContentTablePageViewModel] =
    for {
      title <- nonEmptyString
      heading <- nonEmptySimpleMessage
      inset <- nonEmptySimpleMessage
      buttonText <- nonEmptySimpleMessage
      rows <- Gen.listOf(tupleOf(nonEmptySimpleMessage, nonEmptySimpleMessage))
      onSubmit <- call
    } yield {
      ContentTablePageViewModel(title, heading, inset, buttonText, onSubmit, rows: _*)
    }

  val actionItemGen: Gen[SummaryAction] =
    for {
      content <- nonEmptyString.map(SimpleMessage(_))
      href    <- relativeUrl
      hidden  <- nonEmptyString.map(SimpleMessage(_))
    } yield {
      SummaryAction(content, href, hidden)
    }

  val summaryListRowGen: Gen[CheckYourAnswersRowViewModel] =
    for {
      key     <- nonEmptyString
      value   <- nonEmptyString
      items   <- Gen.listOf(actionItemGen)
    } yield {
      CheckYourAnswersRowViewModel(SimpleMessage(key), SimpleMessage(value), items)
    }

  val checkYourAnswersViewModelGen: Gen[CheckYourAnswersViewModel] =
    for{
      title    <- nonEmptyString
      heading  <- nonEmptyString
      rows     <- Gen.listOf(summaryListRowGen)
      onSubmit <- call
    } yield {
      CheckYourAnswersViewModel(SimpleMessage(title), SimpleMessage(heading), rows, onSubmit)
    }

  val bankAccountViewModelGen: Gen[BankAccountViewModel] =
    for {
      title <- nonEmptySimpleMessage
      heading <- nonEmptySimpleMessage
      paragraph <- nonEmptySimpleMessage
      bankNameHeading <- nonEmptySimpleMessage
      accountNumberHeading <- nonEmptySimpleMessage
      accountNumberHint <- nonEmptySimpleMessage
      sortCodeHeading <- nonEmptySimpleMessage
      sortCodeHint <- nonEmptySimpleMessage
      buttonText <- nonEmptySimpleMessage
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
      title <- nonEmptySimpleMessage
      heading <- nonEmptySimpleMessage
      firstName <- nonEmptySimpleMessage
      lastName <- nonEmptySimpleMessage
      dateOfBirth <- nonEmptySimpleMessage
      dateOfBirthHint <- nonEmptySimpleMessage
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
      text <- nonEmptySimpleMessage
      changeUrl <- relativeUrl
      changeHiddenText <- nonEmptySimpleMessage
      removeUrl <- relativeUrl
      removeHiddenText <- nonEmptySimpleMessage
    } yield ListRow(text, changeUrl, changeHiddenText, removeUrl, removeHiddenText)

  def summaryViewModelGen(showRadios: Boolean = true): Gen[ListViewModel] =
    for {
      title <- nonEmptySimpleMessage
      heading <- nonEmptySimpleMessage
      rows <- Gen.choose(1, 10).flatMap(Gen.listOfN(_, summaryRowGen))
      radioText <- nonEmptySimpleMessage
      insetText <- nonEmptySimpleMessage
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
      title       <- nonEmptyString
      heading     <- nonEmptyString
      description <- Gen.option(nonEmptyString)
      legend      <- nonEmptyString
      onSubmit    <- call
    } yield {
      YesNoPageViewModel(title, heading, description, legend, onSubmit)
    }

  def radioListRowViewModelGen: Gen[RadioListRowViewModel] =
    for {
      content <- nonEmptySimpleMessage
      value   <- nonEmptyString
    } yield {
      RadioListRowViewModel(content, value)
    }

  def radioListViewModelGen: Gen[RadioListViewModel] =
    for {
      title    <- nonEmptySimpleMessage
      heading  <- nonEmptySimpleMessage
      items    <- Gen.listOf(radioListRowViewModelGen)
      onSubmit <- call
    } yield {
      RadioListViewModel(title, heading, items, onSubmit)
    }

  val dateRangeViewModelGen: Gen[DateRangeViewModel] =
    for {
      title          <- nonEmptySimpleMessage
      heading        <- nonEmptySimpleMessage
      description    <- Gen.option(nonEmptySimpleMessage)
      startDateLabel <- nonEmptySimpleMessage
      endDateLabel   <- nonEmptySimpleMessage
      onSubmit       <- call
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
      title <- nonEmptySimpleMessage
      heading <- nonEmptySimpleMessage
      onSubmit <- call
    } yield {
      MoneyViewModel(title, heading, onSubmit)
    }
}
