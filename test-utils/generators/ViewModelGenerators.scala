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

  def pageViewModelGen[A](implicit gen: Gen[A]): Gen[PageViewModel[A]] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyMessage
      description <- Gen.option(nonEmptyBlockMessage)
      page <- gen
      refresh <- Gen.option(Gen.const(1))
      buttonText <- nonEmptyMessage
      onSubmit <- call
    } yield {
      PageViewModel(title, heading, description, page, refresh, buttonText, onSubmit)
    }

  implicit val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      isStartButton <- boolean
      isLargeHeading <- boolean
    } yield {
      ContentPageViewModel(isStartButton, isLargeHeading)
    }

  implicit val contentTablePageViewModelGen: Gen[ContentTablePageViewModel] =
    for {
      inset <- nonEmptyDisplayMessage
      rows <- Gen.listOf(tupleOf(nonEmptyMessage, nonEmptyMessage))
    } yield {
      ContentTablePageViewModel(inset, rows)
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

  implicit val checkYourAnswersViewModelGen: Gen[CheckYourAnswersViewModel] =
    for {
      rows <- Gen.listOf(summaryListRowGen)
    } yield {
      CheckYourAnswersViewModel(rows)
    }

  implicit val bankAccountViewModelGen: Gen[BankAccountViewModel] =
    for {
      bankNameHeading <- nonEmptyMessage
      accountNumberHeading <- nonEmptyMessage
      accountNumberHint <- nonEmptyMessage
      sortCodeHeading <- nonEmptyMessage
      sortCodeHint <- nonEmptyMessage
    } yield BankAccountViewModel(
      bankNameHeading,
      accountNumberHeading,
      accountNumberHint,
      sortCodeHeading,
      sortCodeHint
    )

  implicit val submissionViewModelGen: Gen[SubmissionViewModel] =
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

  implicit val nameDOBViewModelGen: Gen[NameDOBViewModel] =
    for {
      firstName <- nonEmptyMessage
      lastName <- nonEmptyMessage
      dateOfBirth <- nonEmptyMessage
      dateOfBirthHint <- nonEmptyMessage
    } yield NameDOBViewModel(
      firstName,
      lastName,
      dateOfBirth,
      dateOfBirthHint
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
      inset <- nonEmptyDisplayMessage
      rows <- rows.fold(Gen.choose(1, 10))(Gen.const).flatMap(Gen.listOfN(_, summaryRowGen))
      radioText <- nonEmptyMessage
      pagination <- if (paginate) Gen.option(paginationGen) else Gen.const(None)
    } yield ListViewModel(
      inset,
      rows,
      radioText,
      showRadios,
      pagination
    )

  implicit val yesNoPageViewModelGen: Gen[YesNoPageViewModel] =
    for {
      legend <- Gen.option(nonEmptyMessage)
      hint <- Gen.option(nonEmptyMessage)
      yes <- Gen.option(nonEmptyMessage)
      no <- Gen.option(nonEmptyMessage)
    } yield {
      YesNoPageViewModel(legend, hint, yes, no)
    }

  def radioListRowViewModelGen: Gen[RadioListRowViewModel] =
    for {
      content <- nonEmptyMessage
      value <- nonEmptyString
    } yield {
      RadioListRowViewModel(content, value)
    }

  implicit val radioListViewModelGen: Gen[RadioListViewModel] =
    for {
      legend <- Gen.option(nonEmptyMessage)
      items <- Gen.listOf(radioListRowViewModelGen)
    } yield {
      RadioListViewModel(legend, items)
    }

  implicit val dateRangeViewModelGen: Gen[DateRangeViewModel] =
    for {
      startDateLabel <- nonEmptyMessage
      endDateLabel <- nonEmptyMessage
    } yield {
      DateRangeViewModel(
        startDateLabel,
        endDateLabel
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

  implicit val textInputViewModelGen: Gen[TextInputViewModel] =
    for {
      label <- Gen.option(nonEmptyMessage)
    } yield {
      TextInputViewModel(label)
    }

  implicit val uploadViewModelGen: Gen[UploadViewModel] =
    for {
      displayContent <- nonEmptyMessage
      fileSize <- Gen.chooseNum(1, 100)
      formFields <- mapOf(Gen.alphaStr, Gen.alphaStr, 10)
      error <- Gen.option(Gen.alphaStr)
    } yield {
      UploadViewModel(
        displayContent,
        ".csv",
        fileSize.toString,
        formFields,
        error
      )
    }

  implicit val textAreaViewModelGen: Gen[TextAreaViewModel] =
    for {
      rows <- Gen.chooseNum(1, 100)
    } yield {
      TextAreaViewModel(rows)
    }
}
