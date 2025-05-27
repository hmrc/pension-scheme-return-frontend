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

import config.RefinedTypes._
import handlers.UnrefinableIntException
import eu.timepit.refined.refineV
import eu.timepit.refined.api.{Refined, Validate}

object IntUtils {
  implicit class IntOpts(x: Int) {
    def refined[N](implicit v: Validate[Int, OneTo[N]]): Int Refined OneTo[N] =
      refineV[OneTo[N]](x).getOrElse(throw new UnrefinableIntException(s"Failed to refine number: $x"))
    def refined2[Max](implicit v: Validate[Int, Max]): Int Refined Max =
      refineV[Max](x).getOrElse(throw new UnrefinableIntException(s"Failed to refine number: $x"))
  }

  implicit def toInt[N]: Int Refined OneTo[N] => Int = _.value
}
