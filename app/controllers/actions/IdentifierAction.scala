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

package controllers.actions

import config.Constants
import connectors.cache.SessionDataCacheConnector
import models.cache.PensionSchemeUser.{Administrator, Practitioner}
import models.cache.SessionData
import models.requests.IdentifierRequest
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IdentifierAction @Inject()(
  override val authConnector: AuthConnector,
  sessionDataCacheConnector: SessionDataCacheConnector,
  override val parser: BodyParser[AnyContent]
)(implicit override val executionContext: ExecutionContext) extends ActionBuilder[IdentifierRequest, AnyContent] with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(Enrolment(Constants.psaEnrolmentKey) or Enrolment(Constants.pspEnrolmentKey))
      .retrieve(Retrievals.internalId and Retrievals.externalId and Retrievals.allEnrolments) {

        case Some(internalId) ~ Some(externalId) ~ (IsPSA(psaId) && IsPSP(pspId)) =>
          sessionDataCacheConnector.fetch(externalId).flatMap {
            case None => Future.successful(Unauthorized)
            case Some(SessionData(Administrator)) =>
              block(AdministratorRequest(internalId, externalId, request, psaId.value))
            case Some(SessionData(Practitioner)) =>
              block(PractitionerRequest(internalId, externalId, request, pspId.value))
          }

        case Some(internalId) ~ Some(externalId) ~ IsPSA(psaId) =>
          block(AdministratorRequest(internalId, externalId, request, psaId.value))

        case Some(internalId) ~ Some(externalId) ~ IsPSP(pspId) =>
          block(PractitionerRequest(internalId, externalId, request, pspId.value))

        case _ => Future.successful(Unauthorized)
      }
      .recover {
        case _: NoActiveSession => Unauthorized
        case _: AuthorisationException => Unauthorized
      }
  }

  case object IsPSA {
    def unapply(enrolments: Enrolments): Option[EnrolmentIdentifier] = {
      enrolments
        .enrolments
        .find(_.key == Constants.psaEnrolmentKey)
        .flatMap(_.getIdentifier(Constants.psaIdKey))
    }
  }

  case object IsPSP {
    def unapply(enrolments: Enrolments): Option[EnrolmentIdentifier] = {
      enrolments
        .enrolments
        .find(_.key == Constants.pspEnrolmentKey)
        .flatMap(_.getIdentifier(Constants.pspIdKey))
    }
  }

  case object && {
    def unapply[A](a: A): Some[(A, A)] = Some((a, a))
  }
}