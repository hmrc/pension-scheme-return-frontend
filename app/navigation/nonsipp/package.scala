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

package navigation

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV

package object nonsipp {

  // Given a list of indexes, this function will check for the next open index to start a new journey
  // Will return None if the the resulting index is out of bounds of the supplied refined type
  def findNextOpenIndex[A](indexes: List[Int])(implicit ev: Validate[Int, A]): Option[Refined[Int, A]] = {
    val sortedIndex = indexes.sorted
    // Checks existing indexes to see if there is a gap
    // (i.e. If one has been deleted between the first and last index)
    val maybeGap = sortedIndex.zipWithIndex.collectFirst {
      case (index, zipIndex) if index != zipIndex => index
    }

    // If there is a gap in the indexes, set the next index to first one in the gap
    maybeGap match {
      case Some(gapIndex) =>
        // Check if the gap is the first index, if so this means that the original first index was deleted
        if (sortedIndex.indexOf(gapIndex) == 0) {
          refineV[A](1).toOption
        } else {
          // Checks the index prior to the gap index and increments that to get the next index
          // This is done instead of just -1 the gap index in case the gap is larger than 1
          val nextIndex = sortedIndex.applyOrElse(sortedIndex.indexOf(gapIndex) - 1, (_: Int) => gapIndex)
          refineV[A](nextIndex + 2).toOption
        }
      case None =>
        refineV[A](indexes.max + 2).toOption
    }
  }
}
