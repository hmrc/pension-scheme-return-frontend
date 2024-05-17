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

package models.softdelete

import models.requests.psr._
import play.api.libs.json.{Format, Json}
import models._

case class SoftDeletedMember(
  memberDetails: MemberPersonalDetails,
  employerContributions: List[EmployerContributions],
  transfersIn: List[TransfersIn],
  transfersOut: List[TransfersOut],
  pensionSurrendered: Option[SurrenderedBenefits],
  memberLumpSumReceived: Option[MemberLumpSumReceived],
  totalMemberContribution: Option[Money],
  totalAmountPensionPaymentsPage: Option[Money]
)

object SoftDeletedMember {
  private implicit val formatSectionDetails: Format[SectionDetails] = Json.format[SectionDetails]
  private implicit val formatMemberLumpSumReceived: Format[MemberLumpSumReceived] = Json.format[MemberLumpSumReceived]
  private implicit val formatEmployerContributions: Format[EmployerContributions] = Json.format[EmployerContributions]
  private implicit val formatMemberPersonalDetails: Format[MemberPersonalDetails] = Json.format[MemberPersonalDetails]
  private implicit val formatMemberDetails: Format[MemberDetails] = Json.format[MemberDetails]
  implicit val format: Format[SoftDeletedMember] = Json.format[SoftDeletedMember]
}
