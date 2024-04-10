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

package utils

import cats.syntax.traverse._
import eu.timepit.refined.refineV
import eu.timepit.refined.api.{Refined, Validate}

import scala.collection.immutable.SortedMap

object MapUtils {
  implicit class MapOps[A, B](m: Map[A, B]) {
    def sort(implicit refinedOrdering: Ordering[A]): SortedMap[A, B] = SortedMap.from[A, B](m.toList)
  }

  implicit class UserAnswersMapOps[A](m: Map[String, A]) {
    def mapKeysToIndex[Index](implicit ev: Validate[Int, Index]): Option[Map[Refined[Int, Index], A]] =
      m.map {
          case (k, v) =>
            k.toIntOption.flatMap(index => refineV[Index](index + 1).toOption).map(_ -> v)
        }
        .toList
        .sequence
        .map(_.toMap)
  }
}
