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

import play.api.mvc._
import generators.Generators
import models.PensionSchemeId.PsaId
import org.scalatest.OptionValues
import models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class FakePsaIdentifierAction @Inject()(
  val bodyParsers: PlayBodyParsers
)(
  implicit override val executionContext: ExecutionContext
) extends IdentifierAction
    with Generators
    with OptionValues {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(administratorRequestGen(request).map(_.copy(userId = "id", psaId = PsaId("A1234567"))).sample.value)

  override def parser: BodyParser[AnyContent] = bodyParsers.default
}
