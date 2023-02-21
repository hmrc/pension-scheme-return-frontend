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
import models.requests.{AllowedAccessRequest, IdentifierRequest}
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Gen.numChar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest

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

  val establisherGen: Gen[Establisher] =
    for {
      name <- Gen.listOfN(3, nonEmptyString).map(_.mkString(" "))
      kind <- Gen.oneOf(EstablisherKind.Company, EstablisherKind.Individual, EstablisherKind.Partnership)
    } yield Establisher(name, kind)

  val schemeDetailsGen: Gen[SchemeDetails] =
    for {
      srn <- nonEmptyString
      name <- nonEmptyString
      pstr <- nonEmptyString
      status <- schemeStatusGen
      schemeType <- nonEmptyString
      authorisingPsa <- Gen.option(nonEmptyString)
      establishers <- Gen.listOf(establisherGen)
    } yield SchemeDetails(srn, name, pstr, status, schemeType, authorisingPsa, establishers)

  val minimalSchemeDetailsGen: Gen[MinimalSchemeDetails] =
    for {
      name         <- nonEmptyString
      srn          <- srnGen.map(_.value)
      schemeStatus <- schemeStatusGen
      openDate     <- Gen.option(date)
      windUpDate   <- Gen.option(date)
    } yield MinimalSchemeDetails(name, srn, schemeStatus, openDate, windUpDate)

  val listMinimalSchemeDetailsGen: Gen[ListMinimalSchemeDetails] =
    Gen.listOf(minimalSchemeDetailsGen).map(xs => ListMinimalSchemeDetails(xs))



  val pensionSchemeUserGen: Gen[PensionSchemeUser] =
    Gen.oneOf(Administrator, Practitioner)

  val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId)
  val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId)

  val srnGen: Gen[Srn] =
    Gen.listOfN(10, numChar)
       .flatMap { xs =>
         Srn(s"S${xs.mkString}")
           .fold[Gen[Srn]](Gen.fail)(x => Gen.const(x))
       }

  val pstrGen: Gen[Pstr] = nonEmptyString.map(Pstr)

  val schemeIdGen: Gen[SchemeId] = Gen.oneOf(srnGen, pstrGen)

  def practitionerRequestGen[A](request: Request[A]): Gen[PractitionerRequest[A]] =
    for {
      userId     <- nonEmptyString
      externalId <- nonEmptyString
      pspId      <- pspIdGen
    } yield PractitionerRequest(userId, externalId, request, pspId)

  def administratorRequestGen[A](request: Request[A]): Gen[AdministratorRequest[A]] =
    for {
      userId     <- nonEmptyString
      externalId <- nonEmptyString
      psaId      <- psaIdGen
    } yield AdministratorRequest(userId, externalId, request, psaId)

  def identifierRequestGen[A](request: Request[A]): Gen[IdentifierRequest[A]] =
    Gen.oneOf(administratorRequestGen[A](request), practitionerRequestGen[A](request))

  def allowedAccessRequestGen[A](request: Request[A]): Gen[AllowedAccessRequest[A]] =
    for {
      request       <- identifierRequestGen[A](request)
      schemeDetails <- schemeDetailsGen
    } yield AllowedAccessRequest(request, schemeDetails)

  def modeGen: Gen[Mode] = Gen.oneOf(NormalMode, CheckMode)

  implicit val dateRangeGen: Gen[DateRange] =
    for {
      startDate <- date
      endDate   <- date
    } yield DateRange(startDate, endDate)

  val bankAccountGen: Gen[BankAccount] =
    for {
      bankName      <- nonEmptyString
      accountNumber <- Gen.listOfN(8, numChar).map(_.mkString)
      separator     <- Gen.oneOf("", " ", "-")
      pairs          = Gen.listOf(2, numChar)
      sortCode      <- Gen.listOfN(3, pairs).map(_.mkString(separator))
    } yield {
      BankAccount(bankName, accountNumber, sortCode)
    }
}

object ModelGenerators extends ModelGenerators