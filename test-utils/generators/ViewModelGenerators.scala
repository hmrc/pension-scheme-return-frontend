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
import viewmodels.DisplayMessage
import viewmodels.models.{ContentPageViewModel, PensionSchemeViewModel, YesNoPageViewModel}

trait ViewModelGenerators extends BasicGenerators {

  val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      title      <- nonEmptyString
      heading    <- nonEmptyString
      paragraphs <- Gen.listOf(nonEmptyString)
      buttonText <- nonEmptyString
      onSubmit   <- call
    } yield {
      ContentPageViewModel(title, heading, paragraphs, buttonText, onSubmit)
    }

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