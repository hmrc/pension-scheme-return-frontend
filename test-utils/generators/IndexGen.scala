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

package generators

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import org.scalacheck.Gen
import org.scalatest.EitherValues

case class IndexGen[A](min: Int, max: Int)(implicit refined: Validate[Int, A]) extends EitherValues {

  private def refine(gen: Gen[Int]): Gen[Refined[Int, A]] =
    gen.map(refineV(_)).retryUntil(_.isRight).map(_.value)

  val empty: Gen[Refined[Int, A]] = refine(Gen.const(min))

  val partial: Gen[Refined[Int, A]] = refine(Gen.chooseNum(min + 1, max - 1))

  val full = refine(Gen.const(max))

}
