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

import play.api.mvc.JavascriptLiteral

sealed trait Mode {

  // TODO deprecated, remove when it is not used
  def fold[A](normal: => A, check: => A): A = this match {
    case CheckMode => check
    case NormalMode => normal
  }

  def fold[A](normal: => A, check: => A, viewOnly: => A): A = this match {
    case CheckMode => check
    case NormalMode => normal
    case ViewOnlyMode => viewOnly
  }

  val isNormalMode: Boolean = this == NormalMode
  val isCheckMode: Boolean = this == CheckMode
}

case object CheckMode extends Mode
case object NormalMode extends Mode
case object ViewOnlyMode extends Mode

object Mode {

  implicit val jsLiteral: JavascriptLiteral[Mode] = {
    case NormalMode => "NormalMode"
    case CheckMode => "CheckMode"
    case ViewOnlyMode => "ViewOnlyMode"
  }
}
