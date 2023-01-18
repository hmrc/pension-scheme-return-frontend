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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{delete, get, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.time.{Millis, Span}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.test.WireMockSupport
import utils.BaseSpec

import scala.reflect.ClassTag

abstract class BaseConnectorSpec extends BaseSpec with WireMockSupport {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(500, Millis)), interval = scaled(Span(50, Millis)))

  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "metric.enabled" -> false
      )

  protected def injected[A: ClassTag](implicit app: Application): A = app.injector.instanceOf[A]

  def stubGet(url: String, response: ResponseDefinitionBuilder): StubMapping = {
    wireMockServer.stubFor(
      get(urlEqualTo(url))
        .willReturn(response)
    )
  }

  def stubDelete(url: String, response: ResponseDefinitionBuilder): StubMapping = {
    wireMockServer.stubFor(
      delete(urlEqualTo(url))
        .willReturn(response)
    )
  }
}