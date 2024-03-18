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

package models

import utils.WithName
import play.api.mvc.JavascriptLiteral

sealed trait SchemeHoldShare {
  val name: String
}

object SchemeHoldShare extends Enumerable.Implicits {

  case object Acquisition extends WithName("01") with SchemeHoldShare

  case object Contribution extends WithName("02") with SchemeHoldShare

  case object Transfer extends WithName("03") with SchemeHoldShare

  val values: List[SchemeHoldShare] = List(Acquisition, Contribution, Transfer)

  implicit val enumerable: Enumerable[SchemeHoldShare] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[SchemeHoldShare] = (value: SchemeHoldShare) => value.name
}
