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

sealed trait AnswersSavedDisplayVersion {
  val name: String
  val key: String
}
object AnswersSavedDisplayVersion extends Enumerable.Implicits {

  case object NoDisplay extends WithName("noDisplay") with AnswersSavedDisplayVersion {
    override val key: String = "0"
  }
  case object Version1 extends WithName("version1") with AnswersSavedDisplayVersion {
    override val key: String = "1"
  }
  case object Version2 extends WithName("version2") with AnswersSavedDisplayVersion {
    override val key: String = "2"
  }

  val values: List[AnswersSavedDisplayVersion] = List(NoDisplay, Version1, Version2)

  def withNameWithDefault(name: String): AnswersSavedDisplayVersion =
    values.find(_.toString.toLowerCase() == name.toLowerCase()).getOrElse(NoDisplay)

  implicit val enumerable: Enumerable[AnswersSavedDisplayVersion] = Enumerable(values.map(v => (v.toString, v))*)

  implicit val jsLiteral: JavascriptLiteral[AnswersSavedDisplayVersion] = (value: AnswersSavedDisplayVersion) =>
    value.name
}
