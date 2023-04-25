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
import play.api.data.Form
import viewmodels.DisplayMessage.Message
import viewmodels.models.MultipleQuestionsViewModel.{DoubleQuestion, SingleQuestion, TripleQuestion}
import viewmodels.models._

trait ViewModelGenerators extends BasicGenerators {

  val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      contents <- Gen.listOf(nonEmptyBlockMessage)
      buttonText <- nonEmptyMessage
      isStartButton <- boolean
      onSubmit <- call
    } yield {
      ContentPageViewModel(title, heading, contents, buttonText, isStartButton, onSubmit)
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
      buttonText <- nonEmptyMessage
      onSubmit <- call
    } yield {
      CheckYourAnswersViewModel(Message(title), Message(heading), rows, buttonText, onSubmit)
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

  val submissionViewModelGen: Gen[SubmissionViewModel] =
    for {
      title <- nonEmptyMessage
      panelHeading <- nonEmptyMessage
      panelContent <- nonEmptyMessage
      content <- nonEmptyMessage
      whatHappensNextContent <- nonEmptyMessage
    } yield SubmissionViewModel(
      title,
      panelHeading,
      panelContent,
      content,
      whatHappensNextContent
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

  def summaryViewModelGen(rows: Int): Gen[ListViewModel] =
    summaryViewModelGen(rows = Some(rows))

  def summaryViewModelGen(
    showRadios: Boolean = true,
    rows: Option[Int] = None,
    paginate: Boolean = false
  ): Gen[ListViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      rows <- rows.fold(Gen.choose(1, 10))(Gen.const).flatMap(Gen.listOfN(_, summaryRowGen))
      radioText <- nonEmptyMessage
      insetText <- nonEmptyMessage
      pagination <- if (paginate) Gen.option(paginationGen) else Gen.const(None)
      onSubmit <- call
    } yield ListViewModel(
      title,
      heading,
      rows,
      radioText,
      insetText,
      showRadios,
      pagination,
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
      description <- Gen.listOf(nonEmptyBlockMessage)
      legend <- Gen.option(nonEmptyMessage)
      hint <- Gen.option(nonEmptyMessage)
      onSubmit <- call
    } yield {
      YesNoPageViewModel(title, heading, description, legend, hint, onSubmit)
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
      contents <- Gen.listOf(nonEmptyBlockMessage)
      legend <- Gen.option(nonEmptyMessage)
      items <- Gen.listOf(radioListRowViewModelGen)
      onSubmit <- call
    } yield {
      RadioListViewModel(title, heading, contents, legend, items, onSubmit)
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

  def fieldGen: Gen[Field] =
    for {
      label <- nonEmptyInlineMessage
      hint <- Gen.option(nonEmptyInlineMessage)
    } yield {
      Field(label, hint)
    }

  def singleQuestionGen[A](form: Form[A]): Gen[SingleQuestion[A]] =
    fieldGen.map(SingleQuestion(form, _))

  def doubleQuestionGen[A](form: Form[(A, A)]): Gen[DoubleQuestion[A]] =
    for {
      field1 <- fieldGen
      field2 <- fieldGen
    } yield {
      DoubleQuestion(
        form,
        field1,
        field2
      )
    }

  def tripleQuestionGen[A](form: Form[(A, A, A)]): Gen[TripleQuestion[A]] =
    for {
      field1 <- fieldGen
      field2 <- fieldGen
      field3 <- fieldGen
    } yield {
      TripleQuestion(
        form,
        field1,
        field2,
        field3
      )
    }

  def moneyViewModelGen[A](questionsGen: Gen[MultipleQuestionsViewModel[A]]): Gen[MoneyViewModel[_]] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      description <- Gen.option(nonEmptyBlockMessage)
      questions <- questionsGen
      onSubmit <- call
    } yield {
      MoneyViewModel(title, heading, description, questions, onSubmit)
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

  def intViewModelGen[A](questions: Gen[MultipleQuestionsViewModel[A]]): Gen[IntViewModel[_]] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      questions <- questions
      onSubmit <- call
    } yield {
      IntViewModel(title, heading, questions, onSubmit)
    }

  val textAreaViewModelGen: Gen[TextAreaViewModel] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      onSubmit <- call
    } yield {
      TextAreaViewModel(title, heading, onSubmit)
    }
}
