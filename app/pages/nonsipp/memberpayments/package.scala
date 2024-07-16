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

package pages.nonsipp

import pages.nonsipp.memberdetails._
import pages.nonsipp.memberreceivedpcls._
import play.api.libs.json.{__, JsPath}

package object memberpayments {
  object Paths {
    val membersPayments: JsPath = __ \ "membersPayments"
  }

  object Omitted {
    // List of UserAnswer keys to omit during comparison with other UserAnswer member payments sections
    val membersPayments: List[String] = List(
      PensionSchemeMembersPage.key,
      SafeToHardDelete.key,
      MemberStatus.key,
      MemberPaymentsRecordVersionPage.key,
      MemberDetailsCompletedPage.key,
      PclsMemberListPage.key,
      MemberPaymentsRecordVersionPage.key
    )
  }
}
