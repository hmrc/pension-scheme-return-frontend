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

import com.google.inject.ImplementedBy
import config.{Constants, FrontendAppConfig}
import connectors.cache.SessionDataCacheConnector
import controllers.routes
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

@ImplementedBy(classOf[IdentifierActionImpl])
trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent]

class IdentifierActionImpl @Inject()(
  appConfig: FrontendAppConfig,
  override val authConnector: AuthConnector,
  sessionDataCacheConnector: SessionDataCacheConnector,
  playBodyParsers: PlayBodyParsers
)(implicit override val executionContext: ExecutionContext)
  extends IdentifierAction
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(Enrolment(Constants.psaEnrolmentKey) or Enrolment(Constants.pspEnrolmentKey))
      .retrieve(Retrievals.internalId and Retrievals.externalId and Retrievals.allEnrolments) {

        case Some(internalId) ~ Some(externalId) ~ (IsPSA(psaId) && IsPSP(pspId)) =>
          sessionDataCacheConnector.fetch(externalId).flatMap {
            case None =>
              Future.successful(Redirect(appConfig.urls.managePensionsSchemes.adminOrPractitionerUrl))
            case Some(SessionData(Administrator)) =>
              block(AdministratorRequest(internalId, externalId, request, psaId.value))
            case Some(SessionData(Practitioner)) =>
              block(PractitionerRequest(internalId, externalId, request, pspId.value))
          }

        case Some(internalId) ~ Some(externalId) ~ IsPSA(psaId) =>
          block(AdministratorRequest(internalId, externalId, request, psaId.value))

        case Some(internalId) ~ Some(externalId) ~ IsPSP(pspId) =>
          block(PractitionerRequest(internalId, externalId, request, pspId.value))

        case Some(_) ~ Some(_) ~ _ =>
          Future.successful(Redirect(appConfig.urls.managePensionsSchemes.registerUrl))

        case _ => Future.successful(Redirect(routes.UnauthorisedController.onPageLoad))
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(appConfig.urls.loginUrl, Map("continue" -> Seq(appConfig.urls.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(appConfig.urls.managePensionsSchemes.registerUrl)
      }
  }

  override def parser: BodyParser[AnyContent] = playBodyParsers.default

  object IsPSA {
    def unapply(enrolments: Enrolments): Option[EnrolmentIdentifier] = {
      enrolments
        .enrolments
        .find(_.key == Constants.psaEnrolmentKey)
        .flatMap(_.getIdentifier(Constants.psaIdKey))
    }
  }

  object IsPSP {
    def unapply(enrolments: Enrolments): Option[EnrolmentIdentifier] = {
      enrolments
        .enrolments
        .find(_.key == Constants.pspEnrolmentKey)
        .flatMap(_.getIdentifier(Constants.pspIdKey))
    }
  }

  object && {
    def unapply[A](a: A): Some[(A, A)] = Some((a, a))
  }
}