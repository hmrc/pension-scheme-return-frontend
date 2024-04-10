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

package pages.nonsipp.otherassetsdisposal

import config.Refined.{OneTo50, OneTo5000}
import eu.timepit.refined.refineMV
import pages.behaviours.PageBehaviours

class RemoveAssetDisposalPageSpec extends PageBehaviours {

  "RemoveAssetDisposalPage" - {

    val srn = srnGen.sample.value
    val assetIndex = refineMV[OneTo5000](1)
    val disposalIndex = refineMV[OneTo50](1)

    beRetrievable[Boolean](RemoveAssetDisposalPage(srn, assetIndex, disposalIndex))

    beSettable[Boolean](RemoveAssetDisposalPage(srn, assetIndex, disposalIndex))

    beRemovable[Boolean](RemoveAssetDisposalPage(srn, assetIndex, disposalIndex))
  }
}
