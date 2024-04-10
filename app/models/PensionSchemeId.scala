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

package models

import models.PensionSchemeId.{PsaId, PspId}

sealed trait PensionSchemeId { self =>

  val value: String

  def fold[B](f1: PsaId => B, f2: PspId => B): B =
    self match {
      case id @ PsaId(_) => f1(id)
      case id @ PspId(_) => f2(id)
    }

  val isPSP: Boolean = this match {
    case PspId(_) => true
    case _ => false
  }
}

object PensionSchemeId {

  case class PspId(value: String) extends PensionSchemeId

  case class PsaId(value: String) extends PensionSchemeId

}
