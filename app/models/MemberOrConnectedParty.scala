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

sealed trait MemberOrConnectedParty {
  val name: String
}

object MemberOrConnectedParty extends Enumerable.Implicits {

  case object Member extends WithName("member") with MemberOrConnectedParty
  case object ConnectedParty extends WithName("connectedParty") with MemberOrConnectedParty
  case object Neither extends WithName("neither") with MemberOrConnectedParty

  val values: List[MemberOrConnectedParty] = List(Member, ConnectedParty, Neither)

  implicit val enumerable: Enumerable[MemberOrConnectedParty] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[MemberOrConnectedParty] = (value: MemberOrConnectedParty) => value.name
}
