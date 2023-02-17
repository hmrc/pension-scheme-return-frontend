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

import joptsimple.internal.Rows
import org.scalacheck.Gen
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryListRow, Value}
import uk.gov.hmrc.mongo.play.json.CollectionFactory.collection
import viewmodels.models.{CheckYourAnswersViewModel, ContentPageViewModel, ContentTablePageViewModel, PensionSchemeViewModel}

trait ViewModelGenerators extends BasicGenerators {

  val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      title      <- nonEmptyString
      heading    <- nonEmptyString
      paragraphs <- Gen.listOf(nonEmptyString)
      buttonText <- nonEmptyString
      onSubmit   <- getCall
    } yield {
      ContentPageViewModel(title, heading, paragraphs, buttonText, onSubmit)
    }

  val contentTablePageViewModelGen: Gen[ContentTablePageViewModel] =
    for {
      title <- nonEmptyString
      heading <- nonEmptySimpleMessage
      inset <- nonEmptySimpleMessage
      buttonText <- nonEmptySimpleMessage
      rows <- Gen.listOf(tupleOf(nonEmptySimpleMessage, nonEmptySimpleMessage))
      onSubmit <- getCall
    } yield {
      ContentTablePageViewModel(title, heading, inset, buttonText, onSubmit, rows: _*)
    }

  val contentGen: Gen[Content] = nonEmptyString.map(Text)

  val actionItemGen: Gen[ActionItem] =
    for {
      href    <- relativeUrl
      content <- nonEmptyString.map(Text)
      hidden  <- Gen.option(nonEmptyString)
    } yield {
      ActionItem(href, content, hidden)
    }

  val summaryListRowGen: Gen[SummaryListRow] =
    for {
      key     <- contentGen.map(Key(_))
      value   <- contentGen.map(Value(_))
      items   <- Gen.listOf(actionItemGen)
      actions <- Gen.option(Gen.const(Actions(items = items)))
    } yield {
      SummaryListRow(key, value, actions = actions)
    }

  val checkYourAnswersViewModelGen: Gen[CheckYourAnswersViewModel] =
    for{
      title    <- nonEmptyString
      heading  <- nonEmptyString
      rows     <- Gen.listOf(summaryListRowGen)
      onSubmit <- getCall

    } yield {
      CheckYourAnswersViewModel(title, heading, onSubmit, SummaryList(rows))
    }

  val pensionSchemeViewModelGen: Gen[PensionSchemeViewModel] =
    for {
      title    <- nonEmptyString
      heading  <- nonEmptyString
      onSubmit <- getCall
    } yield {
      PensionSchemeViewModel(title, heading, onSubmit)
    }
}