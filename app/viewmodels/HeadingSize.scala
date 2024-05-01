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

package viewmodels

sealed trait HeadingSize

object HeadingSize {
  case object ExtraLarge extends WithCssClass("govuk-heading-xl") with HeadingSize
  case object Large extends WithCssClass("govuk-heading-l") with HeadingSize
  case object Medium extends WithCssClass("govuk-heading-m") with HeadingSize
  case object Small extends WithCssClass("govuk-heading-s") with HeadingSize
}
