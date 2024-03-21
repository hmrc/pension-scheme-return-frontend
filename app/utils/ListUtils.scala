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

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import viewmodels.DisplayMessage.Message

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

    def paginate(currentPage: Int, pageSize: Int): List[A] =
      list.slice((currentPage - 1) * pageSize, ((currentPage - 1) * pageSize) + pageSize)

    def toOption: Option[List[A]] =
      if (list.isEmpty) None
      else Some(list)

    def refine[I: Validate[Int, *]](implicit ev: A <:< String): List[Refined[Int, I]] =
      list.flatMap(a => ev(a).toIntOption.flatMap(index => refineV[I](index + 1).toOption))
  }

  implicit class ListTupStringOps(list: List[(String, String)]) {
    def toMessages: List[(Message, Message)] =
      list.map { case (first, second) => Message(first) -> Message(second) }
  }

  implicit class ListTupOps[A, B](list: List[(A, B)]) {
    // refines the first value of the tuple (_1) for each element in the list
    def refine_1[I: Validate[Int, *]](implicit ev: A <:< String): List[(Refined[Int, I], B)] =
      list.flatMap { case (a, b) => ev(a).toIntOption.flatMap(index => refineV[I](index + 1).toOption.map(_ -> b)) }
  }
}
