/*
 * Copyright 2022 HM Revenue & Customs
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

package filters

import akka.stream.Materializer
import base.SpecBase
import config.FrontendAppConfig
import generators.Generators
import org.scalacheck.Gen.{listOf, nonEmptyListOf}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.Results.Ok
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}

import scala.concurrent.Future
import scala.util.Random

class AllowlistFilterSpec extends SpecBase with ScalaCheckPropertyChecks with Generators with MockitoSugar {

  private val mockMaterializer = mock[Materializer]

  def allowListFilter(
    ips: Seq[String] = Seq(),
    destination: String = "",
    excluded: Seq[String] = Seq(),
    enabled: Boolean = true
  )(implicit app: Application): AllowlistFilter = {
    val configuration = app.injector.instanceOf[Configuration]
    val customConf = Configuration(
      "features.ip-allowlist" -> enabled,
      "filters.allowlist.ips" -> ips,
      "filters.allowlist.destination" -> destination,
      "filters.allowlist.excluded" -> excluded
    )

    new AllowlistFilter(new FrontendAppConfig(configuration ++ customConf), mockMaterializer)
  }

  def fakeRequest(path: String, ip: String) =
    FakeRequest("GET", path).withHeaders("True-Client-IP" -> ip)

  val success: RequestHeader => Future[Result] = _ => Future.successful(Ok("Success"))

  "allow list should" - {
    "allow requests through when" - {
      "True-Client-IP is in list of allowed ips" in runningApplication { implicit app =>
        forAll(nonEmptyListOf(ipAddress)) { ips =>

          val req = fakeRequest("/foo/bar", Random.shuffle(ips).head)
          val filter = allowListFilter(ips)
          val resp = filter.apply(success)(req)

          status(resp) mustBe OK
          contentAsString(resp) mustBe "Success"
        }
      }

      "ip-allowlist feature is disabled" in runningApplication { implicit app =>
        forAll(ipAddress) { ip =>

          val req = fakeRequest("/foo/bar", ip)
          val filter = allowListFilter(enabled = false)
          val resp = filter.apply(success)(req)

          status(resp) mustBe OK
          contentAsString(resp) mustBe "Success"
        }
      }

      "requested path is in excluded paths" in runningApplication { implicit app =>
        forAll(nonEmptyListOf(relativeUrl), ipAddress) { (excluded, ip) =>

          val req = fakeRequest(Random.shuffle(excluded).head, ip)
          val filter = allowListFilter(excluded = excluded)
          val resp = filter.apply(success)(req)

          status(resp) mustBe OK
          contentAsString(resp) mustBe "Success"

        }
      }
    }

    "redirect requests when" - {
      "True-Client-IP is not in list of allowed ips" in runningApplication { implicit app =>
        forAll(listOf(ipAddress), ipAddress, relativeUrl) { (ips, ip, destination) =>
          whenever(!ips.contains(ip)) {

            val req = fakeRequest("/foo/bar", ip)
            val filter = allowListFilter(ips, destination)
            val resp = filter.apply(success)(req)

            status(resp) mustBe SEE_OTHER
            redirectLocation(resp) mustBe Some(destination)
          }
        }
      }
    }
  }
}