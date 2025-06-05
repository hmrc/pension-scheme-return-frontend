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

package connectors.cache

import uk.gov.hmrc.http.HttpReads.Implicits.{readFromJson, readOptionOfNotFound}
import config.FrontendAppConfig
import models.cache.SessionData
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import utils.FutureUtils.FutureOps

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class SessionDataCacheConnector @Inject() (config: FrontendAppConfig, http: HttpClientV2) extends Logging {

  private def url: String = s"${config.pensionsAdministrator}/pension-administrator/journey-cache/session-data-self"

  def fetch(cacheId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SessionData]] =
    http
      .get(url"$url")
      .execute[Option[SessionData]]
      .tapError(t => Future.successful(logger.warn(s"Failed to fetch $cacheId with message ${t.getMessage}")))

}
