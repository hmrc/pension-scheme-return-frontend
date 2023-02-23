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

import org.scalacheck.Gen
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models._
import org.scalacheck.Prop.True

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
      buttonText <- nonEmptySimpleMessage
      radioText <- nonEmptySimpleMessage
      insetText <- nonEmptySimpleMessage
      onChangeText <- nonEmptySimpleMessage
      onRemoveText <- nonEmptySimpleMessage
      onSubmit <- call
    } yield ListViewModel(
      title,
      heading,
      rows,
      buttonText,
      radioText,
      insetText,
      showRadios,
      onChangeText,
      onRemoveText,
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
}
