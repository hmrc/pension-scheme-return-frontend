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

// TODO deprecated - use Mode instead
sealed trait CheckOrChange extends Product with Serializable {
  val name: String

  val isCheck: Boolean = this == CheckOrChange.Check
  val isChange: Boolean = this == CheckOrChange.Change

  def fold[A](check: => A, change: => A): A = this match {
    case CheckOrChange.Check => check
    case CheckOrChange.Change => change
  }
}

object CheckOrChange extends Enumerable.Implicits {

  case object Check extends WithName("check") with CheckOrChange

  case object Change extends WithName("change") with CheckOrChange

  val values: List[CheckOrChange] = List(Check, Change)

  implicit val enumerable: Enumerable[CheckOrChange] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[CheckOrChange] = (value: CheckOrChange) => value.name
}
