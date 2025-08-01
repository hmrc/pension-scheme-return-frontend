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

package generators

import play.api.mvc.Request
import models.HowDisposed.HowDisposed
import config.RefinedTypes.OneTo5000
import models.SchemeId.{Pstr, Srn}
import models.PensionSchemeId.{PsaId, PspId}
import models.{ConditionalYesNo, _}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import eu.timepit.refined.api.Refined
import org.scalacheck.Gen.numChar
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import models.cache.PensionSchemeUser
import eu.timepit.refined.refineV
import models.SchemeStatus._
import org.scalacheck.Gen
import uk.gov.hmrc.domain.Nino
import models.HowSharesDisposed.HowSharesDisposed
import models.requests.{AllowedAccessRequest, IdentifierRequest}
import models.cache.PensionSchemeUser.{Administrator, Practitioner}

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

trait ModelGenerators extends BasicGenerators {

  implicit val sectionCompletedGen: Gen[SectionCompleted.type] = Gen.const(SectionCompleted)

  lazy val minimalDetailsGen: Gen[MinimalDetails] =
    for {
      email <- emailGen
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
      name <- nonEmptyString
      pstr <- nonEmptyString
      status <- schemeStatusGen
      schemeType <- nonEmptyString
      authorisingPsa <- Gen.option(nonEmptyString)
      establishers <- Gen.listOf(establisherGen)
    } yield SchemeDetails(name, pstr, status, schemeType, authorisingPsa, establishers)

  val srnGen: Gen[Srn] =
    Gen
      .listOfN(10, numChar)
      .flatMap { xs =>
        Srn(s"S${xs.mkString}")
          .fold[Gen[Srn]](Gen.fail)(x => Gen.const(x))
      }

  val minimalSchemeDetailsGen: Gen[MinimalSchemeDetails] =
    for {
      name <- nonEmptyString
      srn <- srnGen.map(_.value)
      schemeStatus <- schemeStatusGen
      openDate <- Gen.option(date)
      windUpDate <- Gen.option(date)
    } yield MinimalSchemeDetails(name, srn, schemeStatus, openDate, windUpDate)

  val listMinimalSchemeDetailsGen: Gen[ListMinimalSchemeDetails] =
    Gen.listOf(minimalSchemeDetailsGen).map(xs => ListMinimalSchemeDetails(xs))

  val pensionSchemeUserGen: Gen[PensionSchemeUser] =
    Gen.oneOf(Administrator, Practitioner)

  val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId.apply)
  val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId.apply)
  val pensionSchemeIdGen: Gen[PensionSchemeId] = Gen.oneOf(psaIdGen, pspIdGen)

  val addressGen: Gen[Address] = for {
    addressLine1 <- nonEmptyString
    addressLine2 <- Gen.option(nonEmptyString)
    town <- nonEmptyString
  } yield Address(
    "test-id",
    addressLine1,
    addressLine2,
    None,
    town,
    Some("ZZ1 1ZZ"),
    "United Kingdom",
    "GB",
    LookupAddress
  )

  val pstrGen: Gen[Pstr] = nonEmptyString.map(Pstr.apply)

  val schemeIdGen: Gen[SchemeId] = Gen.oneOf(srnGen, pstrGen)

  def practitionerRequestGen[A](request: Request[A]): Gen[PractitionerRequest[A]] =
    for {
      userId <- nonEmptyString
      externalId <- nonEmptyString
      pspId <- pspIdGen
    } yield PractitionerRequest(userId, externalId, request, pspId)

  def administratorRequestGen[A](request: Request[A]): Gen[AdministratorRequest[A]] =
    for {
      userId <- nonEmptyString
      externalId <- nonEmptyString
      psaId <- psaIdGen
    } yield AdministratorRequest(userId, externalId, request, psaId)

  def identifierRequestGen[A](request: Request[A]): Gen[IdentifierRequest[A]] =
    Gen.oneOf(administratorRequestGen[A](request), practitionerRequestGen[A](request))

  def allowedAccessRequestGen[A](request: Request[A]): Gen[AllowedAccessRequest[A]] =
    for {
      request <- identifierRequestGen[A](request)
      schemeDetails <- schemeDetailsGen
      minimalDetails <- minimalDetailsGen
      srn <- srnGen
    } yield AllowedAccessRequest(request, schemeDetails, minimalDetails, srn)

  def modeGen: Gen[Mode] = Gen.oneOf(NormalMode, CheckMode)

  implicit val dateRangeGen: Gen[DateRange] =
    for {
      startDate <- date
      endDate <- date
    } yield DateRange(startDate, endDate)

  def dateRangeWithinRangeGen(range: DateRange): Gen[DateRange] =
    for {
      date1 <- datesBetween(range.from, range.to)
      date2 <- datesBetween(range.from, range.to)
    } yield
      if (date1.isBefore(date2)) DateRange(date1, date2)
      else DateRange(date2, date1)

  val localDateTimeGen: Gen[LocalDateTime] =
    for {
      seconds <- Gen.chooseNum(
        LocalDateTime.MIN.toEpochSecond(ZoneOffset.UTC),
        LocalDateTime.MAX.toEpochSecond(ZoneOffset.UTC)
      )
      nanos <- Gen.chooseNum(LocalDateTime.MIN.getNano, LocalDateTime.MAX.getNano)
    } yield LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC)

  val manualOrUploadGen: Gen[ManualOrUpload] = Gen.oneOf(ManualOrUpload.values)

  implicit val moneyGen: Gen[Money] = for {
    whole <- Gen.choose(0, 100000)
    decimals <- Gen.option(Gen.choose(0, 99))
  } yield {
    val decimalString = decimals.map(d => s".$d").getOrElse("")
    val result = s"$whole$decimalString".toDouble
    Money(result)
  }

  implicit val moneyInPeriodGen: Gen[MoneyInPeriod] = for {
    moneyAtStart <- moneyGen
    moneyAtEnd <- moneyGen
  } yield MoneyInPeriod(moneyAtStart, moneyAtEnd)

  implicit val pensionCommencementLumpSumGen: Gen[PensionCommencementLumpSum] = for {
    received <- moneyGen
    relevant <- moneyGen
  } yield PensionCommencementLumpSum(received, relevant)

  implicit val percentageGen: Gen[Percentage] = for {
    percentageDouble <- Gen.choose(0.00, 100.00)
  } yield Percentage(percentageDouble)

  val ninoPrefix: Gen[String] =
    (for {
      fst <- Gen.oneOf('A' to 'Z')
      snd <- Gen.oneOf('A' to 'Z')
    } yield s"$fst$snd").retryUntil(s => Nino.isValid(s"${s}000000A"))

  implicit val ninoGen: Gen[Nino] = for {
    prefix <- ninoPrefix
    numbers <- Gen.listOfN(6, Gen.numChar).map(_.mkString)
    suffix <- Gen.oneOf("A", "B", "C", "D")
  } yield Nino(s"$prefix$numbers$suffix")

  implicit val utrGen: Gen[Utr] = for {
    numbers <- Gen.listOfN(10, Gen.numChar).map(_.mkString)
  } yield Utr(s"$numbers")

  val crnPrefix: Gen[String] =
    (for {
      fst <- Gen.oneOf('A' to 'Z')
      snd <- Gen.oneOf('A' to 'Z')
    } yield s"$fst$snd").retryUntil(s => Crn.isValid(s"${s}000000"))

  implicit val crnGen: Gen[Crn] = for {
    prefix <- crnPrefix
    numbers <- Gen.listOfN(6, Gen.numChar).map(_.mkString)
  } yield Crn(s"$prefix$numbers")

  implicit val nameDobGen: Gen[NameDOB] = for {
    firstName <- nonEmptyAlphaString.map(_.take(10))
    lastName <- nonEmptyAlphaString.map(_.take(10))
    dob <- datesBetween(earliestDate, LocalDate.now())
  } yield NameDOB(firstName, lastName, dob)

  implicit val schemeMemberNumbersGen: Gen[SchemeMemberNumbers] = for {
    active <- Gen.chooseNum(0, 99999)
    deferred <- Gen.chooseNum(0, 99999)
    pensioners <- Gen.chooseNum(0, 99999)
  } yield SchemeMemberNumbers(active, deferred, pensioners)

  val wrappedMemberDetailsGen: Gen[WrappedMemberDetails] =
    for {
      nameDob <- nameDobGen
      nino <- Gen.either(nonEmptyString, ninoGen)
    } yield WrappedMemberDetails(nameDob, nino)

  implicit def conditionalYesNoGen[No: Gen, Yes: Gen]: Gen[ConditionalYesNo[No, Yes]] =
    Gen.either(implicitly[Gen[No]], implicitly[Gen[Yes]]).map(ConditionalYesNo(_))

  implicit val memberOrConnectedPartyGen: Gen[MemberOrConnectedParty] =
    Gen.oneOf(MemberOrConnectedParty.Member, MemberOrConnectedParty.ConnectedParty, MemberOrConnectedParty.Neither)

  implicit val schemeHoldLandPropertyGen: Gen[SchemeHoldLandProperty] =
    Gen.oneOf(SchemeHoldLandProperty.Acquisition, SchemeHoldLandProperty.Contribution, SchemeHoldLandProperty.Transfer)

  implicit val schemeHoldSharesGen: Gen[SchemeHoldShare] =
    Gen.oneOf(SchemeHoldShare.Acquisition, SchemeHoldShare.Contribution, SchemeHoldShare.Transfer)

  implicit val schemeHoldBondsGen: Gen[SchemeHoldBond] =
    Gen.oneOf(SchemeHoldBond.Acquisition, SchemeHoldBond.Contribution, SchemeHoldBond.Transfer)

  implicit val schemeHoldAssetsGen: Gen[SchemeHoldAsset] =
    Gen.oneOf(SchemeHoldAsset.Acquisition, SchemeHoldAsset.Contribution, SchemeHoldAsset.Transfer)

  implicit val sponsoringOrConnectedPartyGen: Gen[SponsoringOrConnectedParty] =
    Gen.oneOf(
      SponsoringOrConnectedParty.Sponsoring,
      SponsoringOrConnectedParty.ConnectedParty,
      SponsoringOrConnectedParty.Neither
    )

  implicit val identityTypeGen: Gen[IdentityType] =
    Gen.oneOf(
      IdentityType.UKCompany,
      IdentityType.UKPartnership,
      IdentityType.Individual,
      IdentityType.Other
    )

  implicit val howDisposedGen: Gen[HowDisposed] =
    Gen.oneOf(
      HowDisposed.Sold,
      HowDisposed.Transferred,
      HowDisposed.Other("test details")
    )

  implicit val howSharesDisposedGen: Gen[HowSharesDisposed] =
    Gen.oneOf(
      HowSharesDisposed.Sold,
      HowSharesDisposed.Redeemed,
      HowSharesDisposed.Transferred,
      HowSharesDisposed.Other("test details")
    )

  implicit val typeOfSharesGen: Gen[TypeOfShares] =
    Gen.oneOf(
      TypeOfShares.SponsoringEmployer,
      TypeOfShares.Unquoted,
      TypeOfShares.ConnectedParty
    )

  implicit val Max5000Gen: Gen[Refined[Int, OneTo5000]] =
    Gen.choose(1, 9999999).map(refineV[OneTo5000](_).value)

  implicit val recipientDetailsGen: Gen[RecipientDetails] = for {
    name <- nonEmptyString
    description <- nonEmptyString
  } yield RecipientDetails(name, description)

  implicit val sectionJourneyStatusGen: Gen[SectionJourneyStatus] =
    Gen.oneOf(
      SectionJourneyStatus.InProgress("test url"),
      SectionJourneyStatus.Completed
    )

  val fullAmountOfTheLoanGenerator: Gen[AmountOfTheLoan] = for {
    loanAmount <- moneyGen
    optCapRepaymentCY <- Gen.option(moneyGen)
    optAmountOutstanding <- Gen.option(moneyGen)
  } yield AmountOfTheLoan(loanAmount, optCapRepaymentCY, optAmountOutstanding)

  val partialAmountOfTheLoanGenerator: Gen[AmountOfTheLoan] = for {
    loanAmount <- moneyGen
  } yield AmountOfTheLoan(loanAmount, None, None)

  implicit val amountOfTheLoanGen: Gen[AmountOfTheLoan] =
    Gen.oneOf(fullAmountOfTheLoanGenerator, partialAmountOfTheLoanGenerator)

  val interestOnLoanGenerator: Gen[InterestOnLoan] = for {
    loanInterestAmount <- moneyGen
    loanInterestRate <- percentageGen
    optIntReceivedCY <- Gen.option(moneyGen)
  } yield InterestOnLoan(loanInterestAmount, loanInterestRate, optIntReceivedCY)

  val partialInterestOnLoanGenerator: Gen[InterestOnLoan] = for {
    loanInterestAmount <- moneyGen
    loanInterestRate <- percentageGen
  } yield InterestOnLoan(loanInterestAmount, loanInterestRate, None)

  implicit val interestOnLoanGen: Gen[InterestOnLoan] =
    Gen.oneOf(interestOnLoanGenerator, partialInterestOnLoanGenerator)
}

object ModelGenerators extends ModelGenerators

case class WrappedMemberDetails(nameDob: NameDOB, nino: Either[String, Nino])
