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

package models

import play.api.mvc.JavascriptLiteral
import utils.WithName

sealed trait IdentitySubject {
  val name: String
  val key: String
}
object IdentitySubject extends Enumerable.Implicits {

  case object LoanRecipient extends WithName("loan-recipient") with IdentitySubject {
    override val key: String = "loanRecipient"
  }
  case object LandOrPropertySeller extends WithName("land-or-property-seller") with IdentitySubject {
    override val key: String = "landOrPropertySeller"
  }
  case object Unknown extends WithName("unknown") with IdentitySubject {
    override val key: String = "unknown"
  }

  val values: List[IdentitySubject] = List(LoanRecipient, LandOrPropertySeller)

  def withNameWithDefault(name: String): IdentitySubject =
    values.find(_.toString.toLowerCase() == name.toLowerCase()).getOrElse(Unknown)

  implicit val enumerable: Enumerable[IdentitySubject] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[IdentitySubject] = (value: IdentitySubject) => value.name
}
