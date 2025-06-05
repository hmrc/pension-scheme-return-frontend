/*
 * Copyright 2025 HM Revenue & Customs
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

import config.RefinedTypes.{OneTo, _}
import handlers.UnrefinableIntException
import eu.timepit.refined.refineV
import eu.timepit.refined.api.{Refined, Validate}

object IntUtils {
  extension (x: Int) {
    def refined[N](implicit v: Validate[Int, OneTo[N]]): Int Refined OneTo[N] =
      refineV[OneTo[N]](x).getOrElse(throw new UnrefinableIntException(s"Failed to refine number: $x"))
    def refined2[Max](implicit v: Validate[Int, Max]): Int Refined Max =
      refineV[Max](x).getOrElse(throw new UnrefinableIntException(s"Failed to refine number: $x"))
  }

  given toRefined5000: Conversion[Int, Int Refined OneTo5000] with
    def apply(x: Int): Int Refined OneTo5000 = x.refined
  given toRefined50: Conversion[Int, Int Refined OneTo50] with
    def apply(x: Int): Int Refined OneTo50 = x.refined
  given toRefined300: Conversion[Int, Int Refined OneTo300] with
    def apply(x: Int): Int Refined OneTo300 = x.refined
  given toRefined3: Conversion[Int, Int Refined OneToThree] with
    def apply(x: Int): Int Refined OneToThree = x.refined
  given toRefined5: Conversion[Int, Int Refined OneTo5] with
    def apply(x: Int): Int Refined OneTo5 = x.refined

  given toInt[N]: Conversion[Int Refined OneTo[N], Int] with
    def apply(x: Refined[Int, OneTo[N]]): Int = x.value
}
