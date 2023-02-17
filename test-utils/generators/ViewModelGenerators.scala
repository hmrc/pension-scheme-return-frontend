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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
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

  val checkYourAnswersViewModelGen: Gen[CheckYourAnswersViewModel] =
    for{
      title <- nonEmptyString
      heading <- nonEmptyString
//      rows <- Gen.listOf(SummaryList)
      onSubmit <- getCall

    } yield {
      CheckYourAnswersViewModel(title, heading, onSubmit, SummaryList.defaultObject)
    }

  val pensionSchemeViewModelGen: Gen[PensionSchemeViewModel] =
    for {
      title <- nonEmptyString
      heading <- nonEmptyString
      onSubmit <- getCall
    } yield {
      PensionSchemeViewModel(title, heading, onSubmit)
    }
}