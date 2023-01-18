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

package generators

import models.PensionSchemeId.{PsaId, PspId}
import models.SchemeId.{Pstr, Srn}
import models.SchemeStatus._
import models._
import models.cache.PensionSchemeUser
import models.cache.PensionSchemeUser.{Administrator, Practitioner}
import org.scalacheck.Gen

trait ModelGenerators extends BasicGenerators {
  lazy val minimalDetailsGen: Gen[MinimalDetails] =
    for {
      email <- email
      isSuspended <- boolean
      orgName <- Gen.option(nonEmptyString)
      individual <- Gen.option(individualDetailsGen)
      rlsFlag <- boolean
      deceasedFlag <- boolean
    } yield MinimalDetails(email, isSuspended, orgName, individual, rlsFlag, deceasedFlag)

  lazy val individualDetailsGen: Gen[IndividualDetails] =
    for {
      firstName <- nonEmptyString
      middleName <- Gen.option(nonEmptyString)
      lastName <- nonEmptyString
    } yield IndividualDetails(firstName, middleName, lastName)

  val validSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Open,
      WoundUp,
      Deregistered
    )

  val invalidSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Pending,
      PendingInfoRequired,
      PendingInfoReceived,
      Rejected,
      RejectedUnderAppeal
    )

  val schemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(validSchemeStatusGen, invalidSchemeStatusGen)

  val schemeDetailsGen: Gen[SchemeDetails] =
    for {
      name <- nonEmptyString
      pstr <- nonEmptyString
      status <- schemeStatusGen
      authorisingPsa <- Gen.option(nonEmptyString)
    } yield SchemeDetails(name, pstr, status, authorisingPsa)

  val pensionSchemeUserGen: Gen[PensionSchemeUser] =
    Gen.oneOf(Administrator, Practitioner)

  val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId)
  val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId)

  val srnGen: Gen[Srn] = nonEmptyString.map(Srn)
  val pstrGen: Gen[Pstr] = nonEmptyString.map(Pstr)

  val schemeIdGen: Gen[SchemeId] = Gen.oneOf(srnGen, pstrGen)
}
