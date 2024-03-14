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

import com.google.inject.Singleton
import config.Refined.OneTo5000
import eu.timepit.refined.refineV
import models.SchemeHoldBond.{Acquisition, Transfer}
import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr.{BondTransactions, Bonds}
import models.{Money, UserAnswers}
import pages.nonsipp.bonds._
import viewmodels.models.SectionCompleted

import javax.inject.Inject
import scala.util.Try

@Singleton()
class BondTransactionsTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn)(
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
                totalIncomeOrReceipts = totalIncomeOrReceipts.value
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

          for {
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
            ua8
          }
      }
    } yield resultUA
  }
}
