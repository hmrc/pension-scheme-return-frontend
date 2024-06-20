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
import config.Refined.{Max300, Max5}
import models.SchemeId.Srn
import models.UserAnswers
import pages.nonsipp.membertransferout._
import viewmodels.models.{SectionCompleted, SectionStatus}
import models.requests.psr._
import models.UserAnswers.implicits.UserAnswersTryOps

import scala.util.Try

import javax.inject.Inject

@Singleton()
class TransfersOutTransformer @Inject() extends Transformer {

  def transformToEtmp(srn: Srn, index: Max300, userAnswers: UserAnswers): Option[List[TransfersOut]] = {
    val secondaryIndexes =
      keysToIndex[Max5.Refined](userAnswers.map(TransfersOutCompletedPages(srn, index)))

    secondaryIndexes
      .traverse { secondaryIndex =>
        for {
          schemeName <- userAnswers.get(ReceivingSchemeNamePage(srn, index, secondaryIndex))
          dateOfTransfer <- userAnswers.get(WhenWasTransferMadePage(srn, index, secondaryIndex))
          transferSchemeType <- userAnswers.get(ReceivingSchemeTypePage(srn, index, secondaryIndex))
        } yield TransfersOut(
          schemeName = schemeName,
          dateOfTransfer = dateOfTransfer,
          transferSchemeType = transferSchemeType
        )
      }
  }

  def transformFromEtmp(
    srn: Srn,
    index: Max300,
    transfersOut: List[TransfersOut],
    transfersOutCompleted: Boolean
  ): List[Try[UserAnswers] => Try[UserAnswers]] =
    transfersOut.zipWithIndex.flatMap {
      case (transferOut, zippedIndex) =>
        refineIndex[Max5.Refined](zippedIndex).fold(List.empty[Try[UserAnswers] => Try[UserAnswers]]) {
          secondaryIndex =>
            List[Try[UserAnswers] => Try[UserAnswers]](
              _.set(
                TransfersOutSectionCompleted(srn, index, secondaryIndex),
                SectionCompleted
              ),
              _.set(ReceivingSchemeNamePage(srn, index, secondaryIndex), transferOut.schemeName),
              _.set(WhenWasTransferMadePage(srn, index, secondaryIndex), transferOut.dateOfTransfer),
              _.set(ReceivingSchemeTypePage(srn, index, secondaryIndex), transferOut.transferSchemeType)
            )
        }
    } ++ List(
      _.setWhen(transfersOutCompleted)(SchemeTransferOutPage(srn), transfersOut.nonEmpty),
      _.setWhen(transfersOutCompleted)(TransferOutMemberListPage(srn), true),
      _.setWhen(transfersOutCompleted)(TransfersOutJourneyStatus(srn), SectionStatus.Completed)
    )
}
