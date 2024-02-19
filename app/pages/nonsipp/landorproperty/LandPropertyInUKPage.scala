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

package pages.nonsipp.landorproperty

import config.Refined.{Max5000, OneTo50}
import eu.timepit.refined.refineV
import models.SchemeId.Srn
import models.UserAnswers
import pages.QuestionPage
import pages.nonsipp.landorpropertydisposal.{HowWasPropertyDisposedOfPage, HowWasPropertyDisposedOfPages}
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils.removePages
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class LandPropertyInUKPage(srn: Srn, index: Max5000) extends QuestionPage[Boolean] {

  override def path: JsPath =
    Paths.landOrPropertyTransactions \ "propertyDetails" \ toString \ index.arrayIndex.toString

  override def toString: String = "landOrPropertyInUK"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(true), Some(false)) => removePages(userAnswers, addressPages(srn))
      case (Some(false), Some(true)) => removePages(userAnswers, addressPages(srn))
      case (None, _) =>
        removePages(
          userAnswers,
          pages(srn, userAnswers.map(LandPropertyInUKPages(srn)).size == 1, dependantPages(srn, userAnswers))
        )
      case _ => Try(userAnswers)
    }

  private def addressPages(srn: Srn): List[Removable[_]] =
    List(
      LandOrPropertyPostcodeLookupPage(srn, index),
      LandOrPropertyChosenAddressPage(srn, index),
      AddressLookupResultsPage(srn, index)
    )
  private def dependantPages(srn: Srn, userAnswers: UserAnswers): List[Removable[_]] =
    userAnswers
      .map(HowWasPropertyDisposedOfPages(srn, index))
      .keys
      .toList
      .flatMap(
        key =>
          refineV[OneTo50](key.toInt + 1)
            .fold(_ => Nil, ind => List(HowWasPropertyDisposedOfPage(srn, index, ind)))
      )

  private def pages(srn: Srn, isLastRecord: Boolean, dependentPages: List[Removable[_]]): List[Removable[_]] = {
    val list = List(
      LandOrPropertyChosenAddressPage(srn, index),
      LandRegistryTitleNumberPage(srn, index),
      WhyDoesSchemeHoldLandPropertyPage(srn, index),
      LandOrPropertyTotalCostPage(srn, index),
      IsLandOrPropertyResidentialPage(srn, index),
      IsLandPropertyLeasedPage(srn, index),
      LandOrPropertySellerConnectedPartyPage(srn, index),
      LandOrPropertyTotalIncomePage(srn, index),
      RemovePropertyPage(srn, index),
      LandOrPropertyCompleted(srn, index)
    )
    val list1 = list ++ dependentPages
    if (isLastRecord) list1 :+ LandOrPropertyHeldPage(srn) else list1
  }
}

case class LandPropertyInUKPages(srn: Srn) extends QuestionPage[Map[String, Boolean]] {
  override def path: JsPath =
    Paths.landOrPropertyTransactions \ "propertyDetails" \ toString
  override def toString: String = "landOrPropertyInUK"
}
