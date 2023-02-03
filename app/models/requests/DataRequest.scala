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

package models.requests

import play.api.mvc.{Request, WrappedRequest}
import models.{SchemeDetails, UserAnswers}

case class OptionalDataRequest[A] (request: AllowedAccessRequest[A], userAnswers: Option[UserAnswers]) extends WrappedRequest[A](request) {

  def getUserId: String = request.getUserId

  def schemeDetails: SchemeDetails = request.schemeDetails
}

case class DataRequest[A] (request: AllowedAccessRequest[A], userAnswers: UserAnswers) extends WrappedRequest[A](request) {

  def getUserId: String = request.getUserId
}
