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

package models

import utils.WithName
import play.api.mvc.JavascriptLiteral

sealed trait SchemeHoldLandProperty {
  val name: String
}

object SchemeHoldLandProperty extends Enumerable.Implicits {

  case object Acquisition extends WithName("Acquisition") with SchemeHoldLandProperty

  case object Contribution extends WithName("Contribution") with SchemeHoldLandProperty

  case object Transfer extends WithName("Transfer") with SchemeHoldLandProperty

  val values: List[SchemeHoldLandProperty] = List(Acquisition, Contribution, Transfer)

  implicit val enumerable: Enumerable[SchemeHoldLandProperty] = Enumerable(values.map(v => (v.toString, v))*)

  implicit val jsLiteral: JavascriptLiteral[SchemeHoldLandProperty] = (value: SchemeHoldLandProperty) => value.name
}
