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

package generators

import viewmodels.models.MultipleQuestionsViewModel.{DoubleQuestion, SingleQuestion, TripleQuestion}
import cats.data.NonEmptyList
import org.scalacheck.Gen
import viewmodels.models.TaskListStatus.TaskListStatus
import viewmodels.InputWidth
import play.api.data.{Form, FormError}
import viewmodels.DisplayMessage.Message
import viewmodels.models._

trait ViewModelGenerators extends BasicGenerators {

  def formPageViewModelGen[A](implicit gen: Gen[A]): Gen[FormPageViewModel[A]] =
    for {
      title <- nonEmptyMessage(30)
      heading <- nonEmptyInlineMessage
      description <- Gen.option(nonEmptyBlockMessage)
      page <- gen
      refresh <- Gen.option(Gen.const(1))
      buttonText <- nonEmptyMessage
      details <- Gen.option(furtherDetailsViewModel)
      onSubmit <- call
    } yield FormPageViewModel(title, heading, description, page, refresh, buttonText, details, onSubmit)

  def pageViewModelGen[A](implicit gen: Gen[A]): Gen[PageViewModel[A]] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyInlineMessage
      description <- Gen.option(nonEmptyBlockMessage)
      page <- gen
    } yield PageViewModel(title, heading, description, page)

  implicit val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      isStartButton <- boolean
      isLargeHeading <- boolean
    } yield ContentPageViewModel(isStartButton, isLargeHeading)

  implicit val contentTablePageViewModelGen: Gen[ContentTablePageViewModel] =
    for {
      inset <- nonEmptyDisplayMessage
      before <- nonEmptyParagraphMessage
      after <- nonEmptyParagraphMessage
      rows <- Gen.listOf(tupleOf(nonEmptyMessage, nonEmptyMessage))
    } yield ContentTablePageViewModel(Some(inset), Some(before), Some(after), rows)

  val actionItemGen: Gen[SummaryAction] =
    for {
      content <- nonEmptyString.map(Message(_))
      href <- relativeUrl
      hidden <- nonEmptyString.map(Message(_))
    } yield SummaryAction(content, href, hidden)

  val summaryListRowGen: Gen[CheckYourAnswersRowViewModel] =
    for {
      key <- nonEmptyString
      value <- nonEmptyString
      items <- Gen.listOf(actionItemGen)
    } yield CheckYourAnswersRowViewModel(Message(key), Message(value), items)

  val summaryListSectionGen: Gen[CheckYourAnswersSection] =
    for {
      heading <- Gen.option(nonEmptyMessage)
      rows <- Gen.listOf(summaryListRowGen)
    } yield CheckYourAnswersSection(heading, rows)

  implicit val checkYourAnswersViewModelGen: Gen[CheckYourAnswersViewModel] =
    for {
      sections <- Gen.listOfN(2, summaryListSectionGen)
    } yield CheckYourAnswersViewModel(sections)

  implicit val submissionViewModelGen: Gen[SubmissionViewModel] =
    for {
      title <- nonEmptyMessage
      panelHeading <- nonEmptyMessage
      panelContent <- nonEmptyMessage
      email <- Gen.option(nonEmptyMessage)
      scheme <- nonEmptyMessage
      periodOfReturn <- nonEmptyMessage
      dateSubmitted <- nonEmptyMessage
      whatHappensNextContent <- nonEmptyMessage
    } yield SubmissionViewModel(
      title,
      panelHeading,
      panelContent,
      email,
      scheme,
      periodOfReturn,
      dateSubmitted,
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

  val listRadiosRowGen: Gen[ListRadiosRow] =
    for {
      index <- Gen.chooseNum(1, 100)
      text <- nonEmptyMessage
    } yield ListRadiosRow(index, text)

  def summaryViewModelGen(rows: Int): Gen[ListViewModel] =
    summaryViewModelGen(rows = Some(rows))

  def summaryViewModelGen(
    showRadios: Boolean = true,
    rows: Option[Int] = None,
    paginate: Boolean = false
  ): Gen[ListViewModel] =
    for {
      inset <- nonEmptyDisplayMessage
      nRows <- rows.fold(Gen.choose(1, 10))(Gen.const)
      rows <- Gen.listOfN(nRows, summaryRowGen)
      radioText <- nonEmptyMessage
      pagination <- if (paginate) Gen.option(paginationGen) else Gen.const(None)
      yesHint <- nonEmptyMessage
    } yield ListViewModel(
      inset,
      List(ListSection(rows)),
      radioText,
      showRadios,
      pagination,
      Some(yesHint)
    )

  def listRadiosViewModelGen(
    rows: Option[Int] = None,
    paginate: Boolean = false
  ): Gen[ListRadiosViewModel] =
    for {
      legend <- nonEmptyDisplayMessage
      rows <- rows.fold(Gen.choose(1, 10))(Gen.const).flatMap(Gen.listOfN(_, listRadiosRowGen))
      pagination <- if (paginate) Gen.option(paginationGen) else Gen.const(None)
    } yield ListRadiosViewModel(
      Some(legend),
      rows,
      pagination
    )

  implicit val yesNoPageViewModelGen: Gen[YesNoPageViewModel] =
    for {
      legend <- Gen.option(nonEmptyMessage)
      hint <- Gen.option(nonEmptyMessage)
      yes <- Gen.option(nonEmptyMessage)
      no <- Gen.option(nonEmptyMessage)
      details <- Gen.option(furtherDetailsViewModel)
    } yield YesNoPageViewModel(legend, hint, yes, no, details)

  def furtherDetailsViewModel: Gen[FurtherDetailsViewModel] =
    for {
      title <- nonEmptyMessage(30)
      contents <- nonEmptyDisplayMessage
    } yield FurtherDetailsViewModel(title, contents)

  def radioListRowViewModelGen: Gen[RadioListRowViewModel] =
    for {
      content <- nonEmptyMessage(30)
      value <- uniqueStringGen
    } yield RadioListRowViewModel(content, value)

  implicit val radioListViewModelGen: Gen[RadioListViewModel] =
    for {
      legend <- Gen.option(nonEmptyMessage(30))
      items <- Gen.listOfN(5, radioListRowViewModelGen)
      divider <- Gen.oneOf(Nil, List(RadioListRowDivider("divider")))
    } yield RadioListViewModel(legend, items ++ divider)

  implicit val dateRangeViewModelGen: Gen[DateRangeViewModel] =
    for {
      startDateLabel <- nonEmptyMessage
      startDateHint <- nonEmptyMessage
      endDateLabel <- nonEmptyMessage
      endDateHint <- nonEmptyMessage
    } yield DateRangeViewModel(
      startDateLabel,
      startDateHint,
      endDateLabel,
      endDateHint
    )

  def fieldGen: Gen[QuestionField] =
    for {
      label <- nonEmptyInlineMessage
      hint <- Gen.option(nonEmptyInlineMessage)
      fieldType <- Gen.oneOf(FieldType.Input, FieldType.Date, FieldType.Currency)
    } yield QuestionField(label, hint, Some(InputWidth.Full), Nil, fieldType)

  def singleQuestionGen[A](form: Form[A]): Gen[SingleQuestion[A]] =
    fieldGen.map(SingleQuestion(form, _))

  def doubleQuestionGen[A](form: Form[(A, A)]): Gen[DoubleQuestion[A]] =
    for {
      field1 <- fieldGen
      field2 <- fieldGen
    } yield DoubleQuestion(
      form,
      field1,
      field2
    )

  def tripleQuestionGen[A](form: Form[(A, A, A)]): Gen[TripleQuestion[A, A, A]] =
    for {
      field1 <- fieldGen
      field2 <- fieldGen
      field3 <- fieldGen
    } yield TripleQuestion(
      form,
      field1,
      field2,
      field3
    )

  implicit val textInputViewModelGen: Gen[TextInputViewModel] =
    for {
      label <- Gen.option(nonEmptyMessage)
      isFixedLength <- boolean
    } yield TextInputViewModel(label, isFixedLength)

  implicit val uploadViewModelGen: Gen[UploadViewModel] =
    for {
      displayContent <- nonEmptyMessage
      fileSize <- Gen.chooseNum(1, 100)
      formFields <- mapOf(Gen.alphaStr, Gen.alphaStr, 10)
      error <- Gen.option(Gen.alphaStr)
    } yield UploadViewModel(
      displayContent,
      ".csv",
      fileSize.toString,
      formFields,
      error.map(FormError("file-upload", _))
    )

  implicit val textAreaViewModelGen: Gen[TextAreaViewModel] =
    for {
      rows <- Gen.chooseNum(1, 100)
    } yield TextAreaViewModel(rows)

  val taskListStatusGen: Gen[TaskListStatus] =
    Gen.oneOf(
      TaskListStatus.UnableToStart,
      TaskListStatus.NotStarted,
      TaskListStatus.InProgress,
      TaskListStatus.Completed
    )

  val taskListItemViewModelGen: Gen[TaskListItemViewModel] =
    for {
      link <- nonEmptyLinkMessage
      status <- taskListStatusGen
    } yield TaskListItemViewModel(link, status)

  val taskListSectionViewModelGen: Gen[TaskListSectionViewModel] = {
    val itemsGen = Gen.nonEmptyListOf(taskListItemViewModelGen).map(NonEmptyList.fromList(_).get)

    for {
      sectionTitle <- nonEmptyMessage
      items <- Gen.either(nonEmptyInlineMessage, itemsGen)
      postActionLink <- Gen.option(nonEmptyLinkMessage)
    } yield TaskListSectionViewModel(sectionTitle, items, postActionLink)
  }

  implicit val taskListViewModel: Gen[TaskListViewModel] =
    Gen
      .nonEmptyListOf(taskListSectionViewModelGen)
      .map(NonEmptyList.fromList(_).get)
      .map(TaskListViewModel(false, false, None, Message(""), _))
}
