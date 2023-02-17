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

package models.requests

import models.PensionSchemeId
import models.PensionSchemeId.{PsaId, PspId}
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import play.api.mvc.{Request, WrappedRequest}

sealed abstract class IdentifierRequest[A](request: Request[A]) extends WrappedRequest[A](request) { self =>

  def fold[B](admin: AdministratorRequest[A] => B, practitioner: PractitionerRequest[A] => B): B =
    self match {
      case a: AdministratorRequest[A] => admin(a)
      case p: PractitionerRequest[A]  => practitioner(p)
    }

  def getExternalId: String = fold(_.externalId, _.externalId)
  def getUserId: String = fold(_.userId, _.userId)

  def pensionSchemeId: PensionSchemeId = fold(_.psaId, _.pspId)
}

object IdentifierRequest {

  case class AdministratorRequest[A](
    userId: String,
    externalId: String,
    request: Request[A],
    psaId: PsaId
  ) extends IdentifierRequest[A](request)

  object AdministratorRequest {
    def apply[A](userId: String, externalId: String, request: Request[A], psaId: String): IdentifierRequest[A] =
      AdministratorRequest(userId, externalId, request, PsaId(psaId))
  }

  case class PractitionerRequest[A](
    userId: String,
    externalId: String,
    request: Request[A],
    pspId: PspId
  ) extends IdentifierRequest[A](request)

  object PractitionerRequest {

    def apply[A](userId: String, externalId: String, request: Request[A], pspId: String): IdentifierRequest[A] =
      PractitionerRequest(userId, externalId, request, PspId(pspId))
  }

}