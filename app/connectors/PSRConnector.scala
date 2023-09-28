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

package connectors

import config.FrontendAppConfig
import models.requests.psr.PsrSubmission
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRConnector @Inject()(appConfig: FrontendAppConfig, http: HttpClient) {

  private val baseUrl = appConfig.pensionSchemeReturn.baseUrl

  def submitPsrDetails(
    psrSubmission: PsrSubmission
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    http.POST[PsrSubmission, Unit](
      baseUrl + "/pension-scheme-return/psr/standard",
      psrSubmission
    )
}
