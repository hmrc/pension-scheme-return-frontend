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

import generators.Generators
import models.requests.{AllowedAccessRequest, IdentifierRequest}
import models.{MinimalDetails, SchemeDetails, SchemeId}
import org.scalatest.OptionValues
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeAllowAccessActionProvider @Inject()(schemeDetails: SchemeDetails, minimalDetails: MinimalDetails)
    extends AllowAccessActionProvider
    with Generators
    with OptionValues {

  override def apply(srn: SchemeId.Srn): ActionFunction[IdentifierRequest, AllowedAccessRequest] =
    new ActionFunction[IdentifierRequest, AllowedAccessRequest] {
      override def invokeBlock[A](
        request: IdentifierRequest[A],
        block: AllowedAccessRequest[A] => Future[Result]
      ): Future[Result] =
        block(AllowedAccessRequest(request, schemeDetails, minimalDetails, srn))

      override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
}
