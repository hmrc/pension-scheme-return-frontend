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

package models

import utils.Transform
import play.api.libs.json.{Format, Json}

case class SchemeMemberNumbers(noOfActiveMembers: Int, noOfDeferredMembers: Int, noOfPensionerMembers: Int) {

  val totalActiveAndDeferred: Int = noOfActiveMembers + noOfDeferredMembers
}

object SchemeMemberNumbers {

  implicit val format: Format[SchemeMemberNumbers] = Json.format[SchemeMemberNumbers]

  implicit val transform: Transform[(Int, Int, Int), SchemeMemberNumbers] =
    new Transform[(Int, Int, Int), SchemeMemberNumbers] {

      override def to(a: (Int, Int, Int)): SchemeMemberNumbers =
        SchemeMemberNumbers(a._1, a._2, a._3)

      override def from(b: SchemeMemberNumbers): (Int, Int, Int) =
        (b.noOfActiveMembers, b.noOfDeferredMembers, b.noOfPensionerMembers)
    }
}
