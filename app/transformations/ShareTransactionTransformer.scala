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
import models.SchemeHoldShare.{Acquisition, Transfer}
import models.SchemeId.Srn
import models.TypeOfShares.{SponsoringEmployer, Unquoted}
import models.requests.DataRequest
import models.requests.psr._
import models.{IdentitySubject, IdentityType, PropertyAcquiredFrom, SchemeHoldShare}
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.shares._

import javax.inject.Inject

@Singleton()
class ShareTransactionTransformer @Inject() extends Transformer {

  def transformToEtmp(
    srn: Srn
  )(implicit request: DataRequest[_]): Option[List[ShareTransaction]] =
    request.userAnswers
      .get(TypeOfSharesHeldPages(srn))
      .map { jsValue =>
        jsValue.keys.toList
          .flatMap { key =>
            key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
              case None => None
              case Some(index) =>
                for {
                  typeOfSharesHeld <- request.userAnswers.get(TypeOfSharesHeldPage(srn, index))
                  schemeHoldShare <- request.userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index))
                  nameOfSharesCompany <- request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index))
                  eitherSharesCompanyCrnOrReason <- request.userAnswers.get(SharesCompanyCrnPage(srn, index))
                  classOfShares <- request.userAnswers.get(ClassOfSharesPage(srn, index))
                  totalShares <- request.userAnswers.get(HowManySharesPage(srn, index))
                  costOfShares <- request.userAnswers.get(CostOfSharesPage(srn, index))
                  supportedByIndepValuation <- request.userAnswers.get(SharesIndependentValuationPage(srn, index))
                  totalDividendsOrReceipts <- request.userAnswers.get(SharesTotalIncomePage(srn, index))

                } yield {

                  val optDateOfAcqOrContrib = Option.when(schemeHoldShare != Transfer)(
                    request.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)).get
                  )

                  val optConnectedPartyStatus = Option.when(typeOfSharesHeld == Unquoted)(
                    request.userAnswers.get(SharesFromConnectedPartyPage(srn, index)).get
                  )

                  val optTotalAssetValue =
                    Option.when((schemeHoldShare == Acquisition) && (typeOfSharesHeld == SponsoringEmployer))(
                      request.userAnswers.get(TotalAssetValuePage(srn, index)).get
                    )

                  val optAcquisitionRelatedDetails = buildOptAcquisitionRelatedDetails(schemeHoldShare, srn, index)

                  ShareTransaction(
                    typeOfSharesHeld = typeOfSharesHeld,
                    shareIdentification = ShareIdentification(
                      nameOfSharesCompany = nameOfSharesCompany,
                      optCrnNumber = eitherSharesCompanyCrnOrReason.value.toOption.map(_.crn),
                      optReasonNoCRN = eitherSharesCompanyCrnOrReason.value.left.toOption,
                      classOfShares = classOfShares
                    ),
                    heldSharesTransaction = HeldSharesTransaction(
                      schemeHoldShare = schemeHoldShare,
                      optDateOfAcqOrContrib = optDateOfAcqOrContrib,
                      totalShares = totalShares,
                      optAcquiredFromName = optAcquisitionRelatedDetails.map(_._1),
                      optPropertyAcquiredFrom = optAcquisitionRelatedDetails.map(_._2),
                      optConnectedPartyStatus = optConnectedPartyStatus,
                      costOfShares = costOfShares.value,
                      supportedByIndepValuation = supportedByIndepValuation,
                      optTotalAssetValue = optTotalAssetValue.map(_.value),
                      totalDividendsOrReceipts = totalDividendsOrReceipts.value
                    )
                  )
                }
            }
          }
      }

  private def buildOptAcquisitionRelatedDetails(
    schemeHoldShare: SchemeHoldShare,
    srn: Srn,
    index: Refined[Int, OneTo5000]
  )(implicit request: DataRequest[_]): Option[(String, PropertyAcquiredFrom)] =
    Option
      .when(schemeHoldShare == Acquisition) {
        val identityType =
          request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.SharesSeller)).get

        identityType match {
          case IdentityType.Individual =>
            (
              request.userAnswers.get(IndividualNameOfSharesSellerPage(srn, index)),
              request.userAnswers.get(SharesIndividualSellerNINumberPage(srn, index)).map(_.value)
            ).mapN {
              case (name, Right(nino)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = Some(nino.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noNinoReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noNinoReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.UKCompany =>
            (
              request.userAnswers.get(CompanyNameOfSharesSellerPage(srn, index)),
              request.userAnswers
                .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller))
                .map(_.value)
            ).mapN {
              case (name, Right(crn)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = Some(crn.value),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noCrnReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noCrnReason),
                    otherDescription = None
                  )
                )
            }

          case IdentityType.UKPartnership =>
            (
              request.userAnswers.get(PartnershipShareSellerNamePage(srn, index)),
              request.userAnswers
                .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller))
                .map(_.value)
            ).mapN {
              case (name, Right(utr)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = Some(utr.value.filterNot(_.isWhitespace)),
                    reasonNoIdNumber = None,
                    otherDescription = None
                  )
                )
              case (name, Left(noUtrReason)) =>
                (
                  name,
                  PropertyAcquiredFrom(
                    identityType = identityType,
                    idNumber = None,
                    reasonNoIdNumber = Some(noUtrReason),
                    otherDescription = None
                  )
                )
            }
          case IdentityType.Other =>
            request.userAnswers
              .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller))
              .map(
                other =>
                  (
                    other.name,
                    PropertyAcquiredFrom(
                      identityType = identityType,
                      idNumber = None,
                      reasonNoIdNumber = None,
                      otherDescription = Some(other.description)
                    )
                  )
              )

        }
      }
      .flatten
}
