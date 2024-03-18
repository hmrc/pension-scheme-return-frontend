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

package models

import play.api.libs.json._
import utils.WithName

sealed trait PointOfEntry {
  val pointOfEntry: String
}

object PointOfEntry {
  case object NoPointOfEntry extends WithName("NoPointOfEntry") with PointOfEntry {
    val pointOfEntry = "NoPointOfEntry"
  }

  case object HowWereSharesDisposedPointOfEntry
      extends WithName("HowWereSharesDisposedPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "HowWereSharesDisposedPointOfEntry"
  }

  case object WhoWereTheSharesSoldToPointOfEntry
      extends WithName("WhoWereTheSharesSoldToPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "WhoWereTheSharesSoldToPointOfEntry"
  }

  case object HowWereBondsDisposedPointOfEntry extends WithName("HowWereBondsDisposedPointOfEntry") with PointOfEntry {
    val pointOfEntry = "HowWereBondsDisposedPointOfEntry"
  }

  case object AssetAcquisitionToContributionPointOfEntry
      extends WithName("AssetAcquisitionToContributionPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "AssetAcquisitionToContributionPointOfEntry"
  }

  case object AssetAcquisitionToTransferPointOfEntry
      extends WithName("AssetAcquisitionToTransferPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "AssetAcquisitionToTransferPointOfEntry"
  }

  case object AssetContributionToAcquisitionPointOfEntry
      extends WithName("AssetContributionToAcquisitionPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "AssetContributionToAcquisitionPointOfEntry"
  }

  case object AssetContributionToTransferPointOfEntry
      extends WithName("AssetContributionToTransferPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "AssetContributionToTransferPointOfEntry"
  }

  case object AssetTransferToAcquisitionPointOfEntry
      extends WithName("AssetTransferToAcquisitionPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "AssetTransferToAcquisitionPointOfEntry"
  }

  case object AssetTransferToContributionPointOfEntry
      extends WithName("AssetTransferToContributionPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "AssetTransferToContributionPointOfEntry"
  }

  case object WhoWasAssetAcquiredFromPointOfEntry
      extends WithName("WhoWasAssetAcquiredFromPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "WhoWasAssetAcquiredFromPointOfEntry"
  }

  implicit val format: Format[PointOfEntry] = new Format[PointOfEntry] {
    override def reads(json: JsValue): JsResult[PointOfEntry] = json match {
      case JsString(NoPointOfEntry.name) => JsSuccess(NoPointOfEntry)
      // Shares Disposal
      case JsString(HowWereSharesDisposedPointOfEntry.name) => JsSuccess(HowWereSharesDisposedPointOfEntry)
      case JsString(WhoWereTheSharesSoldToPointOfEntry.name) => JsSuccess(WhoWereTheSharesSoldToPointOfEntry)
      // Bonds Disposal
      case JsString(HowWereBondsDisposedPointOfEntry.name) => JsSuccess(HowWereBondsDisposedPointOfEntry)
      // Other Assets
      case JsString(AssetAcquisitionToContributionPointOfEntry.name) =>
        JsSuccess(AssetAcquisitionToContributionPointOfEntry)
      case JsString(AssetAcquisitionToTransferPointOfEntry.name) =>
        JsSuccess(AssetAcquisitionToTransferPointOfEntry)
      case JsString(AssetContributionToAcquisitionPointOfEntry.name) =>
        JsSuccess(AssetContributionToAcquisitionPointOfEntry)
      case JsString(AssetContributionToTransferPointOfEntry.name) =>
        JsSuccess(AssetContributionToTransferPointOfEntry)
      case JsString(AssetTransferToAcquisitionPointOfEntry.name) =>
        JsSuccess(AssetTransferToAcquisitionPointOfEntry)
      case JsString(AssetTransferToContributionPointOfEntry.name) =>
        JsSuccess(AssetTransferToContributionPointOfEntry)
      case JsString(WhoWasAssetAcquiredFromPointOfEntry.name) => JsSuccess(WhoWasAssetAcquiredFromPointOfEntry)
      case unknown => JsError(s"Unknown PointOfEntry value: $unknown")
    }

    override def writes(o: PointOfEntry): JsValue = JsString(o.pointOfEntry)
  }
}
