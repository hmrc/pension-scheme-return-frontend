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
import config.Refined.OneTo5000
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import models.SchemeHoldLandProperty.{Acquisition, Transfer}
import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr._
import models.{IdentitySubject, IdentityType, SchemeHoldLandProperty}
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.landorproperty._
import play.api.libs.json.JsObject

import java.time.LocalDate
import javax.inject.Inject

@Singleton()
class LandOrPropertyTransactionsTransformer @Inject()() {

  def transform(srn: Srn)(implicit request: DataRequest[_]): List[LandOrPropertyTransactions] =
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
