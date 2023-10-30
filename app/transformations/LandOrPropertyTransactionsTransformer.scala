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

package transformations

import cats.implicits.catsSyntaxTuple2Semigroupal
import com.google.inject.Singleton
import config.Refined.{Max5000, OneTo5000}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import models.IdentitySubject.LandOrPropertySeller
import models.SchemeHoldLandProperty.{Acquisition, Transfer}
import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr._
import models.{
  ConditionalYesNo,
  Crn,
  IdentitySubject,
  IdentityType,
  Money,
  RecipientDetails,
  SchemeHoldLandProperty,
  UserAnswers,
  Utr
}
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.landorproperty._
import play.api.libs.json.JsObject
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import javax.inject.Inject
import scala.util.Try

@Singleton()
class LandOrPropertyTransactionsTransformer @Inject()() extends Transformer {

  def transformToEtmp(srn: Srn)(implicit request: DataRequest[_]): List[LandOrPropertyTransactions] =
    request.userAnswers
      .get(Paths.landOrPropertyTransactions \ "propertyDetails" \ "landOrPropertyInUK")
      .map { jsValue =>
        jsValue
          .as[JsObject]
          .keys
          .map { key =>
            key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
              case None => None
              case Some(index) =>
                for {
                  landOrPropertyInUK <- request.userAnswers.get(LandPropertyInUKPage(srn, index))
                  addressDetails <- request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index))
                  landRegistryTitleNumber <- request.userAnswers.get(LandRegistryTitleNumberPage(srn, index))
                  methodOfHolding <- request.userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, index))

                  totalCostOfLandOrProperty <- request.userAnswers.get(LandOrPropertyTotalCostPage(srn, index))
                  isLandOrPropertyResidential <- request.userAnswers.get(IsLandOrPropertyResidentialPage(srn, index))
                  landOrPropertyLeased <- request.userAnswers.get(IsLandPropertyLeasedPage(srn, index))
                  totalIncomeOrReceipts <- request.userAnswers.get(LandOrPropertyTotalIncomePage(srn, index))
                } yield {

                  val optNoneTransferRelatedDetails = buildOptNoneTransferRelatedDetails(methodOfHolding, srn, index)
                  val optAcquisitionRelatedDetails = buildOptAcquisitionRelatedDetails(methodOfHolding, srn, index)
                  val optLandOrPropertyLeasedDetails =
                    buildOptLandOrPropertyLeasedDetails(landOrPropertyLeased, srn, index)

                  LandOrPropertyTransactions(
                    PropertyDetails(
                      landOrPropertyInUK = landOrPropertyInUK,
                      addressDetails = addressDetails,
                      landRegistryTitleNumberKey = landRegistryTitleNumber.value.isRight,
                      landRegistryTitleNumberValue = landRegistryTitleNumber.value.merge
                    ),
                    HeldPropertyTransaction(
                      methodOfHolding = methodOfHolding,
                      dateOfAcquisitionOrContribution = optNoneTransferRelatedDetails.map(_._2),
                      optPropertyAcquiredFromName = optAcquisitionRelatedDetails.map(_._1),
                      optPropertyAcquiredFrom = optAcquisitionRelatedDetails.map(_._3),
                      optConnectedPartyStatus = optAcquisitionRelatedDetails.map(_._2),
                      totalCostOfLandOrProperty = totalCostOfLandOrProperty.value,
                      optIndepValuationSupport = optNoneTransferRelatedDetails.map(_._1),
                      isLandOrPropertyResidential = isLandOrPropertyResidential,
                      optLeaseDetails = optLandOrPropertyLeasedDetails,
                      landOrPropertyLeased = landOrPropertyLeased,
                      totalIncomeOrReceipts = totalIncomeOrReceipts.value
                    )
                  )
                }
            }
          }
          .toList
          .flatten
      }
      .getOrElse(List.empty)

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, landOrProperty: LandOrProperty): Try[UserAnswers] = {
    val landOrPropertyTransactions = landOrProperty.landOrPropertyTransactions

    for {
      indexes <- buildIndexesForMax5000(landOrPropertyTransactions.size)
      resultUA <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          val propertyDetails = landOrPropertyTransactions(index.value - 1).propertyDetails
          val heldPropertyTransaction = landOrPropertyTransactions(index.value - 1).heldPropertyTransaction

          val landOrPropertyInUK = LandPropertyInUKPage(srn, index) -> propertyDetails.landOrPropertyInUK
          val addressDetails = LandOrPropertyAddressLookupPage(srn, index) -> propertyDetails.addressDetails

          val landRegistryTitleNumber = buildLandRegistryTitleNumberDetail(srn, index, propertyDetails)
          val methodOfHolding = WhyDoesSchemeHoldLandPropertyPage(srn, index) -> heldPropertyTransaction.methodOfHolding
          val totalCostOfLandOrProperty = LandOrPropertyTotalCostPage(srn, index) -> Money(
            heldPropertyTransaction.totalCostOfLandOrProperty
          )
          val isLandOrPropertyResidential = IsLandOrPropertyResidentialPage(srn, index) -> heldPropertyTransaction.isLandOrPropertyResidential
          val landOrPropertyLeased = IsLandPropertyLeasedPage(srn, index) -> heldPropertyTransaction.landOrPropertyLeased
          val totalIncomeOrReceipts = LandOrPropertyTotalIncomePage(srn, index) -> Money(
            heldPropertyTransaction.totalIncomeOrReceipts
          )
          val optDateOfAcquisitionOrContribution = heldPropertyTransaction.dateOfAcquisitionOrContribution.map(
            date => LandOrPropertyWhenDidSchemeAcquirePage(srn, index) -> date
          )
          val optIndepValuationSupport = heldPropertyTransaction.optIndepValuationSupport.map(
            indepValuationSupport => LandPropertyIndependentValuationPage(srn, index) -> indepValuationSupport
          )
          val optSellerConnectedParty = heldPropertyTransaction.optConnectedPartyStatus.map(
            status => LandOrPropertySellerConnectedPartyPage(srn, index) -> status
          )
          val optReceivedLandType = heldPropertyTransaction.optPropertyAcquiredFrom.map(
            prop => {
              IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller) -> prop.identityType
            }
          )
          val optIndividualTuple = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.Individual)
            .map(
              prop => {
                val name = LandPropertyIndividualSellersNamePage(srn, index) -> heldPropertyTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Nino](Nino(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Nino](
                      prop.reasonNoIdNumber.get
                    )
                  )

                (name, IndividualSellerNiPage(srn, index) -> yesNoValue)
              }
            )

          val optUKCompanyTuple = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.UKCompany)
            .map(
              prop => {
                val name = CompanySellerNamePage(srn, index) -> heldPropertyTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Crn](Crn(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Crn](
                      prop.reasonNoIdNumber.get
                    )
                  )
                (name, CompanyRecipientCrnPage(srn, index, LandOrPropertySeller) -> yesNoValue)
              }
            )

          val optUKPartnershipTuple = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.UKPartnership)
            .map(
              prop => {
                val name = PartnershipSellerNamePage(srn, index) -> heldPropertyTransaction.optPropertyAcquiredFromName.get
                val yesNoValue = prop.idNumber
                  .map(id => ConditionalYesNo.yes[String, Utr](Utr(id)))
                  .getOrElse(
                    ConditionalYesNo.no[String, Utr](
                      prop.reasonNoIdNumber.get
                    )
                  )

                (name, PartnershipRecipientUtrPage(srn, index, LandOrPropertySeller) -> yesNoValue)
              }
            )

          val optOther = heldPropertyTransaction.optPropertyAcquiredFrom
            .filter(prop => prop.identityType == IdentityType.Other)
            .map(
              prop => {
                OtherRecipientDetailsPage(srn, index, LandOrPropertySeller) -> RecipientDetails(
                  heldPropertyTransaction.optPropertyAcquiredFromName.get,
                  prop.otherDescription.get
                )
              }
            )

          val optLeaseTuple = heldPropertyTransaction.optLeaseDetails.map(
            leaseDetails => {
              val ldp = LandOrPropertyLeaseDetailsPage(srn, index) -> (leaseDetails.lesseeName, Money(
                leaseDetails.annualLeaseAmount
              ), leaseDetails.leaseGrantDate)
              val leaseConnectedParty = IsLesseeConnectedPartyPage(srn, index) -> leaseDetails.connectedPartyStatus
              (ldp, leaseConnectedParty)
            }
          )

          for {
            ua0 <- ua
            ua1 <- ua0.set(LandOrPropertyHeldPage(srn), true)
            ua2 <- ua1.set(landOrPropertyInUK._1, landOrPropertyInUK._2)
            ua3 <- ua2.set(addressDetails._1, addressDetails._2)
            ua4 <- ua3.set(landRegistryTitleNumber._1, landRegistryTitleNumber._2)
            ua5 <- ua4.set(methodOfHolding._1, methodOfHolding._2)
            ua6 <- ua5.set(totalCostOfLandOrProperty._1, totalCostOfLandOrProperty._2)
            ua7 <- ua6.set(isLandOrPropertyResidential._1, isLandOrPropertyResidential._2)
            ua8 <- ua7.set(landOrPropertyLeased._1, landOrPropertyLeased._2)
            ua9 <- ua8.set(totalIncomeOrReceipts._1, totalIncomeOrReceipts._2)

            ua10 <- optDateOfAcquisitionOrContribution.map(t => ua9.set(t._1, t._2)).getOrElse(Try(ua9))
            ua11 <- optIndepValuationSupport.map(t => ua10.set(t._1, t._2)).getOrElse(Try(ua10))
            ua12 <- optReceivedLandType.map(t => ua11.set(t._1, t._2)).getOrElse(Try(ua11))

            ua13 <- optIndividualTuple.map(t => ua12.set(t._1._1, t._1._2)).getOrElse(Try(ua12))
            ua14 <- optIndividualTuple.map(t => ua13.set(t._2._1, t._2._2)).getOrElse(Try(ua13))
            ua15 <- optUKCompanyTuple.map(t => ua14.set(t._1._1, t._1._2)).getOrElse(Try(ua14))
            ua16 <- optUKCompanyTuple.map(t => ua15.set(t._2._1, t._2._2)).getOrElse(Try(ua15))
            ua17 <- optUKPartnershipTuple.map(t => ua16.set(t._1._1, t._1._2)).getOrElse(Try(ua16))
            ua18 <- optUKPartnershipTuple.map(t => ua17.set(t._2._1, t._2._2)).getOrElse(Try(ua17))
            ua19 <- optOther.map(t => ua18.set(t._1, t._2)).getOrElse(Try(ua18))

            ua20 <- optLeaseTuple.map(t => ua19.set(t._1._1, t._1._2)).getOrElse(Try(ua19))
            ua21 <- optLeaseTuple.map(t => ua20.set(t._2._1, t._2._2)).getOrElse(Try(ua20))
            ua22 <- optSellerConnectedParty.map(t => ua21.set(t._1, t._2)).getOrElse(Try(ua21))

          } yield ua22
      }
    } yield resultUA
  }

  private def buildLandRegistryTitleNumberDetail(srn: Srn, index: Max5000, propertyDetails: PropertyDetails) = {
    val yesOrNo = propertyDetails.landRegistryTitleNumberKey
    val value = propertyDetails.landRegistryTitleNumberValue
    val yesNoValue = if (yesOrNo) {
      ConditionalYesNo.yes[String, String](
        value
      )
    } else {
      ConditionalYesNo.no[String, String](
        value
      )
    }
    LandRegistryTitleNumberPage(srn, index) -> yesNoValue
  }

  private def buildOptNoneTransferRelatedDetails(
    methodOfHolding: SchemeHoldLandProperty,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(Boolean, LocalDate)] = Option.when(methodOfHolding != Transfer) {
    val landPropertyIndependentValuation =
      request.userAnswers.get(LandPropertyIndependentValuationPage(srn, index)).get
    val landOrPropertyAcquire =
      request.userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, index)).get
    (
      landPropertyIndependentValuation,
      landOrPropertyAcquire
    )
  }

  private def buildOptAcquisitionRelatedDetails(
    methodOfHolding: SchemeHoldLandProperty,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(String, Boolean, PropertyAcquiredFrom)] =
    Option
      .when(methodOfHolding == Acquisition) {
        val receivedLandType =
          request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller)).get
        val sellerConnectedParty =
          request.userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, index)).get

        receivedLandType match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(LandPropertyIndividualSellersNamePage(srn, index)),
              request.userAnswers.get(IndividualSellerNiPage(srn, index)).map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanySellerNamePage(srn, index)),
              request.userAnswers
                .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipSellerNamePage(srn, index)),
              request.userAnswers
                .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.LandOrPropertySeller))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  sellerConnectedParty,
                  PropertyAcquiredFrom(
                    identityType = receivedLandType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller))
              .map(
                other =>
                  (
                    other.name,
                    sellerConnectedParty,
                    PropertyAcquiredFrom(
                      identityType = receivedLandType,
                      idNumber = None,
                      reasonNoIdNumber = None,
                      otherDescription = Some(other.description)
                    )
                  )
              )

        }
      }
      .flatten

  private def buildOptLandOrPropertyLeasedDetails(
    landOrPropertyLeased: Boolean,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[LeaseDetails] =
    Option.when(landOrPropertyLeased) {
      val leaseDetails =
        request.userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, index)).get
      val leaseConnectedParty = request.userAnswers.get(IsLesseeConnectedPartyPage(srn, index)).get
      LeaseDetails(
        lesseeName = leaseDetails._1,
        leaseGrantDate = leaseDetails._3,
        annualLeaseAmount = leaseDetails._2.value,
        connectedPartyStatus = leaseConnectedParty
      )
    }
}
