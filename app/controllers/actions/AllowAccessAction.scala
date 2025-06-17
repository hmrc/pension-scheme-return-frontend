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

package controllers.actions

import play.api.mvc.{ActionFunction, Result}
import com.google.inject.ImplementedBy
import connectors.{MinimalDetailsConnector, MinimalDetailsError, SchemeDetailsConnector}
import controllers.routes
import config.FrontendAppConfig
import models.SchemeId.Srn
import models.{MinimalDetails, SchemeDetails, SchemeStatus}
import play.api.mvc.Results.Redirect
import connectors.MinimalDetailsError.DelimitedAdmin
import models.SchemeStatus.{Deregistered, Open, WoundUp}
import play.api.http.Status.FORBIDDEN
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.{AllowedAccessRequest, IdentifierRequest}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class AllowAccessAction(
  srn: Srn,
  appConfig: FrontendAppConfig,
  schemeDetailsConnector: SchemeDetailsConnector,
  minimalDetailsConnector: MinimalDetailsConnector
)(implicit override val executionContext: ExecutionContext)
    extends ActionFunction[IdentifierRequest, AllowedAccessRequest] {

  private val validStatuses: List[SchemeStatus] = List(Open, WoundUp, Deregistered)

  override def invokeBlock[A](
    request: IdentifierRequest[A],
    block: AllowedAccessRequest[A] => Future[Result]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    (for {
      schemeDetails <- fetchSchemeDetails(request, srn)
      minimalDetails <- fetchMinimalDetails(request)
    } yield (schemeDetails, minimalDetails) match {
      case (Some(schemeDetails), Right(minimalDetails @ MinimalDetails(_, _, _, _, false, false)))
          if validStatuses.contains(schemeDetails.schemeStatus) =>
        block(AllowedAccessRequest(request, schemeDetails, minimalDetails, srn))

      case (_, Right(HasDeceasedFlag(_))) =>
        Future.successful(Redirect(appConfig.urls.managePensionsSchemes.contactHmrc))

      case (_, Right(HasRlsFlag(_))) =>
        request.fold(
          _ => Future.successful(Redirect(appConfig.urls.pensionAdministrator.updateContactDetails)),
          _ => Future.successful(Redirect(appConfig.urls.pensionPractitioner.updateContactDetails))
        )

      case (_, Left(DelimitedAdmin)) =>
        Future.successful(Redirect(appConfig.urls.managePensionsSchemes.cannotAccessDeregistered))

      case _ =>
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }).flatten.recoverWith { case UpstreamErrorResponse(_, FORBIDDEN, _, _) =>
      Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }

  private def fetchSchemeDetails[A](request: IdentifierRequest[A], srn: Srn)(implicit
    hc: HeaderCarrier
  ): Future[Option[SchemeDetails]] =
    request.fold(
      a => schemeDetailsConnector.details(a.psaId, srn),
      p => schemeDetailsConnector.details(p.pspId, srn)
    )

  private def fetchMinimalDetails[A](
    request: IdentifierRequest[A]
  )(implicit hc: HeaderCarrier): Future[Either[MinimalDetailsError, MinimalDetails]] =
    request.fold(
      _ => minimalDetailsConnector.fetch(loggedInAsPsa = true),
      _ => minimalDetailsConnector.fetch(loggedInAsPsa = false)
    )

  object HasRlsFlag {
    def unapply(minimalDetails: MinimalDetails): Option[MinimalDetails] =
      if (minimalDetails.rlsFlag) Some(minimalDetails) else None
  }

  object HasDeceasedFlag {
    def unapply(minimalDetails: MinimalDetails): Option[MinimalDetails] =
      if (minimalDetails.deceasedFlag) Some(minimalDetails) else None
  }
}

@ImplementedBy(classOf[AllowAccessActionProviderImpl])
trait AllowAccessActionProvider {
  def apply(srn: Srn): ActionFunction[IdentifierRequest, AllowedAccessRequest]
}

class AllowAccessActionProviderImpl @Inject() (
  appConfig: FrontendAppConfig,
  schemeDetailsConnector: SchemeDetailsConnector,
  minimalDetailsConnector: MinimalDetailsConnector
)(implicit val ec: ExecutionContext)
    extends AllowAccessActionProvider {

  def apply(srn: Srn): ActionFunction[IdentifierRequest, AllowedAccessRequest] =
    new AllowAccessAction(srn, appConfig, schemeDetailsConnector, minimalDetailsConnector)
}
