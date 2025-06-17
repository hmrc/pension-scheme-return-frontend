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

package handlers

import play.api.mvc.{RequestHeader, Result}
import play.twirl.api.Html
import views.html.ErrorTemplate
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import play.api.{Logger, PlayException}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class ErrorHandler @Inject() (
  val messagesApi: MessagesApi,
  view: ErrorTemplate
)(implicit override protected val ec: ExecutionContext)
    extends FrontendErrorHandler
    with I18nSupport {

  private val logger = Logger(getClass)

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    rh: RequestHeader
  ): Future[Html] =
    Future.successful(view(pageTitle, heading, message))

  private def logError(request: RequestHeader, ex: Throwable): Unit =
    logger.error(
      """
        |
        |! %sInternal PSR server error, for (%s) [%s] ->
        | """.stripMargin.format(
        ex match {
          case p: PlayException => "@" + p.id + " - "
          case _ => ""
        },
        request.method,
        request.uri
      ),
      ex
    )

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logError(request, exception)
    exception match {
      case GetPsrException(_, continueUrl, answersSavedDisplayVersion) =>
        Future.successful(
          Redirect(
            controllers.routes.JourneyRecoveryController
              .onPageLoad(
                continueUrl = Some(RedirectUrl(continueUrl)),
                answersSavedDisplayVersion.key.toInt
              )
          )
        )
      case PostPsrException(_, continueUrl) =>
        Future.successful(
          Redirect(
            controllers.routes.JourneyRecoveryController
              .onPageLoad(
                continueUrl = Some(RedirectUrl(continueUrl)),
                2
              )
          )
        )

      case _ => super.onServerError(request, exception)
    }
  }
}
