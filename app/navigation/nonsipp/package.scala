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
  def findNextOpenIndex[A](list: List[Int])(implicit ev: Validate[Int, A]): Option[Refined[Int, A]] = {
    val sortedList = list.sorted
    if (sortedList.isEmpty || sortedList.head != 0) {
      refineV[A](1).toOption
    } else {
      LazyList.from(0).find(index => !sortedList.contains(index)).flatMap(i => refineV[A](i + 1).toOption)
    }
  }
}
