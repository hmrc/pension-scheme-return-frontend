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

package models

import models.GenericFormMapper.ConditionalRadioMapper

trait GenericFormMapper[From, To] {
  def to(a: From): To
  def from(b: To): Option[From]
}

object GenericFormMapper {
  type ConditionalRadioMapper[Conditional, A] = GenericFormMapper[(String, Option[Conditional]), A]

  def apply[A, B](fa: A => B, fb: B => Option[A]): GenericFormMapper[A, B] = new GenericFormMapper[A, B] {
    override def to(a: A): B = fa(a)
    override def from(b: B): Option[A] = fb(b)
  }
}

object ConditionalRadioMapper {
  def apply[A, B](
    to: (String, Option[A]) => B,
    from: B => Option[(String, Option[A])]
  ): ConditionalRadioMapper[A, B] = GenericFormMapper(to.tupled(_), from(_))
}
