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

import cats.syntax.traverse._
import com.google.inject.Singleton
import config.RefinedTypes.{Max300, Max5}
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer._
import models.{Money, UserAnswers}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import models.requests.psr._
import models.UserAnswers.implicits.UserAnswersTryOps

import scala.util.Try

import javax.inject.Inject

@Singleton()
class TransfersInTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, index: Max300, userAnswers: UserAnswers): Option[List[TransfersIn]] = {
    val secondaryIndexes =
      keysToIndex[Max5.Refined](userAnswers.map(TransfersInSectionCompletedForMember(srn, index)))

    secondaryIndexes
      .traverse { secondaryIndex =>
        for {
          schemeName <- userAnswers.get(TransferringSchemeNamePage(srn, index, secondaryIndex))
          dateOfTransfer <- userAnswers.get(WhenWasTransferReceivedPage(srn, index, secondaryIndex))
          transferValue <- userAnswers.get(TotalValueTransferPage(srn, index, secondaryIndex))
          transferIncludedAsset <- userAnswers.get(DidTransferIncludeAssetPage(srn, index, secondaryIndex))
          transferSchemeType <- userAnswers.get(TransferringSchemeTypePage(srn, index, secondaryIndex))
        } yield TransfersIn(
          schemeName = schemeName,
          dateOfTransfer = dateOfTransfer,
          transferSchemeType = transferSchemeType,
          transferValue = transferValue.value,
          transferIncludedAsset = transferIncludedAsset
        )
      }
  }

  def transformFromEtmp(
    srn: Srn,
    index: Max300,
    transfersIn: List[TransfersIn]
  ): List[Try[UserAnswers] => Try[UserAnswers]] =
    transfersIn.zipWithIndex.flatMap { case (transferIn, zippedIndex) =>
      refineIndex[Max5.Refined](zippedIndex).fold(List.empty[UserAnswers.Compose]) { secondaryIndex =>
        List[UserAnswers.Compose](
          _.set(
            TransfersInSectionCompleted(srn, index, secondaryIndex),
            SectionCompleted
          ),
          _.set(TransferringSchemeNamePage(srn, index, secondaryIndex), transferIn.schemeName),
          _.set(WhenWasTransferReceivedPage(srn, index, secondaryIndex), transferIn.dateOfTransfer),
          _.set(TotalValueTransferPage(srn, index, secondaryIndex), Money(transferIn.transferValue)),
          _.set(DidTransferIncludeAssetPage(srn, index, secondaryIndex), transferIn.transferIncludedAsset),
          _.set(TransferringSchemeTypePage(srn, index, secondaryIndex), transferIn.transferSchemeType),
          _.set(ReportAnotherTransferInPage(srn, index, secondaryIndex), false),
          _.set(ReceiveTransferProgress(srn, index, secondaryIndex), SectionJourneyStatus.Completed)
        )
      }
    }
}
