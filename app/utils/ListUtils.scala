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

package utils

import viewmodels.DisplayMessage.SimpleMessage

object ListUtils {

  implicit class ListOps[A](list: List[A]) {
    def intersperse(separator: A, step: Int = 1): List[A] = {

      def go(xs: List[A], iterator: Int): List[A] = xs match {
        case List(x) => List(x)
        case Nil => Nil
        case y :: ys if iterator == 1 => y +: separator +: go(ys, step)
        case y :: ys => y +: go(ys, iterator - 1)
      }

      go(list, step)
    }

    def maybeAppend(maybeElem: Option[A]): List[A] =
      maybeElem.fold(list)(elem => list :+ elem)

    def :?+(maybeElem: Option[A]): List[A] = maybeAppend(maybeElem)

    def removeAt(index: Int): List[A] =
      list.patch(index, Nil, 1)
  }

  implicit class ListTupStringOps(list: List[(String, String)]) {
    def toSimpleMessages: List[(SimpleMessage, SimpleMessage)] =
      list.map { case (first, second) => SimpleMessage(first) -> SimpleMessage(second) }
  }
}
