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

/* Typeclass for transforming type A into B and type B into type A */
trait Transform[A, B] {
  def to(a: A): B

  def from(b: B): A
}

object Transform {

  def apply[A, B](implicit ev: Transform[A, B]): Transform[A, B] = ev

  def instance[A, B](_to: A => B, _from: B => A): Transform[A, B] = new Transform[A, B] {
    override def to(a: A): B = _to(a)

    override def from(b: B): A = _from(b)
  }

  implicit def identity[A]: Transform[A, A] = new Transform[A, A] {
    override def to(a: A): A = a

    override def from(a: A): A = a
  }

  implicit class TransformOps[A](val a: A) extends AnyVal {

    def to[B](implicit ev: Transform[A, B]): B = ev.to(a)

    def from[B](implicit ev: Transform[B, A]): B = ev.from(a)
  }
}
