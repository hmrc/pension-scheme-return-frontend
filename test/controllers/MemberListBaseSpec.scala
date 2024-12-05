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

package controllers

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{Security => _}
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import org.scalatest.Assertion

trait MemberListBaseSpec {
  val submittedOn = "Submitted on"
  val govBackLink = "govuk-back-link"
  val actions = "Actions"

  def checkContent(content: String): Assertion = {
    content must include(submittedOn)
    (content must Matchers.not).include(govBackLink)
    val count = content.sliding(actions.length).count(window => window == actions)
    count mustBe 1
  }
}
