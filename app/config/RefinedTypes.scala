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

package config

import eu.timepit.refined.boolean.And
import models.Enumerable
import eu.timepit.refined.numeric.{Greater, LessEqual}
import eu.timepit.refined.api.Refined

object RefinedTypes {
  type OneTo[N] = Greater[0] And LessEqual[N]

  type OneToThree = Greater[0] And LessEqual[3]
  type Max3 = Int Refined OneToThree

  type OneTo300 = Greater[0] And LessEqual[300]
  type Max300 = Int Refined OneTo300

  type OneTo5000 = Greater[0] And LessEqual[5000]
  type Max5000 = Int Refined OneTo5000

  type OneTo50 = Greater[0] And LessEqual[50]
  type Max50 = Int Refined OneTo50

  type OneTo5 = Greater[0] And LessEqual[5]
  type Max5 = Int Refined OneTo5

  // used by generators
  object Max300 {
    type Refined = Greater[0] And LessEqual[300]
  }

  object Max5000 {
    type Refined = Greater[0] And LessEqual[5000]

    implicit val enumerable: Enumerable[Max5000] = Enumerable.index(1 to 5000)
  }

  object Max50 {
    type Refined = Greater[0] And LessEqual[50]

    implicit val enumerable: Enumerable[Max50] = Enumerable.index(1 to 50)
  }

  object Max5 {
    type Refined = Greater[0] And LessEqual[5]

    implicit val enumerable: Enumerable[Max5] = Enumerable.index(1 to 5)
  }
}
