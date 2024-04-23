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

package transformations

import pages.nonsipp.bonds._
import config.Refined.{Max5000, OneTo50, OneTo5000}
import models.SchemeHoldBond.{Acquisition, Transfer}
import models.SchemeId.Srn
import models.requests.psr.{BondDisposed, BondTransactions, Bonds}
import eu.timepit.refined.refineV
import models.{HowDisposed, Money, UserAnswers}
import pages.nonsipp.bondsdisposal._
import viewmodels.models.SectionCompleted
import models.requests.DataRequest
import eu.timepit.refined.api.Refined
import models.HowDisposed.{Other, Sold}
import com.google.inject.Singleton

import scala.util.Try

import javax.inject.Inject

@Singleton()
class BondTransactionsTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, bondsDisposal: Boolean)(
    implicit request: DataRequest[_]
  ): List[BondTransactions] =
    request.userAnswers
      .map(IncomeFromBondsPages(srn))
      .keys
      .toList
      .flatMap { key =>
        key.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
          case None => None
          case Some(index) =>
            for {
              nameOfBonds <- request.userAnswers.get(NameOfBondsPage(srn, index))
              methodOfHolding <- request.userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, index))
              costOfBonds <- request.userAnswers.get(CostOfBondsPage(srn, index))
              bondsUnregulated <- request.userAnswers.get(AreBondsUnregulatedPage(srn, index))
              totalIncomeOrReceipts <- request.userAnswers.get(IncomeFromBondsPage(srn, index))
            } yield {
              val dateOfAcqOrContrib = Option.when(methodOfHolding != Transfer)(
                request.userAnswers.get(WhenDidSchemeAcquireBondsPage(srn, index)).get
              )
              val connectedPartyStatus = Option.when(methodOfHolding == Acquisition)(
                request.userAnswers.get(BondsFromConnectedPartyPage(srn, index)).get
              )
              BondTransactions(
                nameOfBonds = nameOfBonds,
                methodOfHolding = methodOfHolding,
                optDateOfAcqOrContrib = dateOfAcqOrContrib,
                costOfBonds = costOfBonds.value,
                optConnectedPartyStatus = connectedPartyStatus,
                bondsUnregulated = bondsUnregulated,
                totalIncomeOrReceipts = totalIncomeOrReceipts.value,
                optBondsDisposed = Option
                  .when(bondsDisposal)(buildOptBondsDisposed(srn, index))
              )
            }
        }
      }

  private def buildOptBondsDisposed(
    srn: Srn,
    bondIndex: Refined[Int, OneTo5000]
  )(
    implicit request: DataRequest[_]
  ): Seq[BondDisposed] =
    request.userAnswers
      .map(
        HowWereBondsDisposedOfPagesForEachBond(srn, bondIndex)
      )
      .keys
      .toList
      .flatMap { key =>
        key.toIntOption.flatMap(i => refineV[OneTo50](i + 1).toOption) match {
          case None => None
          case Some(index) =>
            for {
              howWereBondsDisposed <- request.userAnswers.get(
                HowWereBondsDisposedOfPage(srn, bondIndex, index)
              )
              bondsStillHeldPage <- request.userAnswers.get(
                BondsStillHeldPage(srn, bondIndex, index)
              )

            } yield {

              BondDisposed(
                methodOfDisposal = howWereBondsDisposed.name,
                optOtherMethod = howWereBondsDisposed match {
                  case Other(details) => Some(details)
                  case _ => None
                },
                optDateSold = Option.when(howWereBondsDisposed == Sold)(
                  request.userAnswers
                    .get(
                      WhenWereBondsSoldPage(srn, bondIndex, index)
                    )
                    .get
                ),
                optAmountReceived = Option.when(howWereBondsDisposed == Sold)(
                  request.userAnswers
                    .get(
                      TotalConsiderationSaleBondsPage(srn, bondIndex, index)
                    )
                    .get
                    .value
                ),
                optBondsPurchaserName = Option.when(howWereBondsDisposed == Sold)(
                  request.userAnswers
                    .get(
                      BuyerNamePage(srn, bondIndex, index)
                    )
                    .get
                ),
                optConnectedPartyStatus = Option.when(howWereBondsDisposed == Sold)(
                  request.userAnswers
                    .get(
                      IsBuyerConnectedPartyPage(srn, bondIndex, index)
                    )
                    .get
                ),
                totalNowHeld = bondsStillHeldPage
              )
            }
        }
      }

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, bond: Bonds): Try[UserAnswers] = {
    val bondTransactions = bond.bondTransactions
    for {
      indexes <- buildIndexesForMax5000(bondTransactions.size)
      resultUA <- indexes.foldLeft(userAnswers.set(UnregulatedOrConnectedBondsHeldPage(srn), bond.bondsWereAdded)) {
        case (ua, index) =>
          val bondTransaction = bondTransactions(index.value - 1)

          val nameOfBonds = NameOfBondsPage(srn, index) -> bondTransaction.nameOfBonds
          val methodOfHolding = WhyDoesSchemeHoldBondsPage(srn, index) -> bondTransaction.methodOfHolding
          val costOfBonds = CostOfBondsPage(srn, index) -> Money(bondTransaction.costOfBonds)
          val bondsUnregulated = AreBondsUnregulatedPage(srn, index) -> bondTransaction.bondsUnregulated
          val totalIncomeOrReceipts = IncomeFromBondsPage(srn, index) -> Money(bondTransaction.totalIncomeOrReceipts)

          val optDateOfAcqOrContrib = bondTransaction.optDateOfAcqOrContrib.map(
            date => WhenDidSchemeAcquireBondsPage(srn, index) -> date
          )
          val optConnectedPartyStatus = bondTransaction.optConnectedPartyStatus.map(
            connectedPartyStatus => BondsFromConnectedPartyPage(srn, index) -> connectedPartyStatus
          )
          val bondsCompleted = BondsCompleted(srn, index) -> SectionCompleted

          val triedUA = for {
            ua0 <- ua
            ua1 <- ua0.set(nameOfBonds._1, nameOfBonds._2)
            ua2 <- ua1.set(methodOfHolding._1, methodOfHolding._2)
            ua3 <- ua2.set(costOfBonds._1, costOfBonds._2)
            ua4 <- ua3.set(bondsUnregulated._1, bondsUnregulated._2)
            ua5 <- ua4.set(totalIncomeOrReceipts._1, totalIncomeOrReceipts._2)
            ua6 <- optDateOfAcqOrContrib.map(t => ua5.set(t._1, t._2)).getOrElse(Try(ua5))
            ua7 <- optConnectedPartyStatus.map(t => ua6.set(t._1, t._2)).getOrElse(Try(ua6))
            ua8 <- ua7.set(bondsCompleted._1, bondsCompleted._2)
          } yield {
            buildOptDisposedBondsUA(
              index,
              srn,
              ua8,
              bondTransaction.optBondsDisposed,
              bond.bondsWereDisposed
            )
          }
          triedUA.flatten
      }
    } yield resultUA
  }

  private def buildOptDisposedBondsUA(
    index: Max5000,
    srn: Srn,
    userAnswers: UserAnswers,
    optBondsDisposed: Option[Seq[BondDisposed]],
    bondsWereDisposed: Boolean
  ): Try[UserAnswers] = {
    val initialUserAnswersOfDisposal = userAnswers.set(BondsDisposalPage(srn), bondsWereDisposed)
    optBondsDisposed
      .map(bondsDisposed => {
        for {
          disposalIndexes <- buildIndexesForMax50(bondsDisposed.size)
          resultDisposalUA <- disposalIndexes.foldLeft(initialUserAnswersOfDisposal) {
            case (disposalUA, disposalIndex) =>
              val bondDisposed = bondsDisposed(disposalIndex.value - 1)
              val methodOfDisposal = bondDisposed.methodOfDisposal match {
                case HowDisposed.Sold.name => HowDisposed.Sold
                case HowDisposed.Transferred.name => HowDisposed.Transferred
                case HowDisposed.Other.name =>
                  HowDisposed.Other(bondDisposed.optOtherMethod.get)
              }

              val howWereBondsDisposed = HowWereBondsDisposedOfPage(srn, index, disposalIndex) -> methodOfDisposal
              val totalNowHeld = BondsStillHeldPage(srn, index, disposalIndex) -> bondDisposed.totalNowHeld

              val optWhenWereBondsSoldPage = bondDisposed.optDateSold.map(
                date => WhenWereBondsSoldPage(srn, index, disposalIndex) -> date
              )
              val optAmountReceived = bondDisposed.optAmountReceived.map(
                amountReceived => TotalConsiderationSaleBondsPage(srn, index, disposalIndex) -> Money(amountReceived)
              )
              val optBondsPurchaserName = bondDisposed.optBondsPurchaserName.map(
                bondsPurchaserName => BuyerNamePage(srn, index, disposalIndex) -> bondsPurchaserName
              )
              val optConnectedParty = bondDisposed.optConnectedPartyStatus.map(
                status => IsBuyerConnectedPartyPage(srn, index, disposalIndex) -> status
              )

              for {
                disposalUA0 <- disposalUA
                disposalUA1 <- disposalUA0.set(BondsDisposalProgress(srn, index, disposalIndex), SectionCompleted)
                disposalUA2 <- disposalUA1.set(howWereBondsDisposed._1, howWereBondsDisposed._2)
                disposalUA3 <- disposalUA2.set(totalNowHeld._1, totalNowHeld._2)
                disposalUA4 <- optWhenWereBondsSoldPage
                  .map(t => disposalUA3.set(t._1, t._2))
                  .getOrElse(Try(disposalUA3))
                disposalUA5 <- optAmountReceived
                  .map(t => disposalUA4.set(t._1, t._2))
                  .getOrElse(Try(disposalUA4))
                disposalUA6 <- optBondsPurchaserName
                  .map(t => disposalUA5.set(t._1, t._2))
                  .getOrElse(Try(disposalUA5))
                disposalUA7 <- optConnectedParty
                  .map(t => disposalUA6.set(t._1, t._2))
                  .getOrElse(Try(disposalUA6))
              } yield disposalUA7
          }
        } yield resultDisposalUA
      })
      .getOrElse(initialUserAnswersOfDisposal)
  }
}
