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

/* Typeclass for transforming type A into B and type B into type A */
trait Transform[A, B] {
  def to(a: A): B

  def from(b: B): A
}

object Transform {

  implicit def identity[A]: Transform[A, A] = new Transform[A, A] {
    override def to(a: A): A = a

    override def from(a: A): A = a
  }
}
