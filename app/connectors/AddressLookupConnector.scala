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

package connectors

import uk.gov.hmrc.http.HttpReads.Implicits._
import config.FrontendAppConfig
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import models._
import utils.JsonUtils._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class AddressLookupConnector @Inject()(http: HttpClient, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) {

  private val addressLookupUrl = appConfig.addressLookup.baseUrl + "/lookup"

  def lookup(postcode: String, filter: Option[String])(implicit hc: HeaderCarrier): Future[List[ALFAddressResponse]] =
    http
      .POST[JsObject, List[ALFAddressResponse]](
        addressLookupUrl,
        Json.obj("postcode" -> postcode) +? filter.map(f => Json.obj("filter" -> f)),
        headers = List(
          "Content-Type" -> "application/json"
        )
      )
}
