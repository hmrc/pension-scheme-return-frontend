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
import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.mvc.{Call, RequestHeader, Result}
import uk.gov.hmrc.allowlist.AkamaiAllowlistFilter

import scala.concurrent.Future

class AllowlistFilter @Inject()(
  config: FrontendAppConfig,
  override val mat: Materializer) extends AkamaiAllowlistFilter {
  override def allowlist: Seq[String] = config.filters.allowlist.ips

  override def destination: Call = {
    val path = config.filters.allowlist.destination
    Call("GET", path)
  }

  override val excludedPaths: Seq[Call] =
    config.filters.allowlist.excluded.map { e =>
      Call("GET", e)
    }

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    if(config.features.ipAllowlist) {
      super.apply(f)(rh)
    } else {
      f(rh)
    }
  }
}