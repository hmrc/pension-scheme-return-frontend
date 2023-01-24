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

import play.api.mvc._
import play.api.test.Helpers.stubBodyParser

import scala.concurrent.{ExecutionContext, Future}

class FakeActionBuilder[F[_]](fa: F[_]) extends ActionBuilder[F, AnyContent] {
  override def parser: BodyParser[AnyContent] = stubBodyParser[AnyContent]()

  override def invokeBlock[A](request: Request[A], block: F[A] => Future[Result]): Future[Result] = {
    block(fa.asInstanceOf[F[A]])
  }

  override protected def executionContext: ExecutionContext = ExecutionContext.global
}