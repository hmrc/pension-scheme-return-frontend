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

package pages.nonsipp.otherassetsdisposal

import config.Refined.{Max50, Max5000}
import eu.timepit.refined.refineMV
import models.ConditionalYesNo
import pages.behaviours.PageBehaviours
import uk.gov.hmrc.domain.Nino

class AssetIndividualBuyerNiNumberPageSpec extends PageBehaviours {
  private val srn = srnGen.sample.value

  "AssetIndividualBuyerNiNumberPage" - {

    val assetIndex = refineMV[Max5000.Refined](1)
    val disposalIndex = refineMV[Max50.Refined](1)

    beRetrievable[ConditionalYesNo[String, Nino]](AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex))

    beSettable[ConditionalYesNo[String, Nino]](AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex))

    beRemovable[ConditionalYesNo[String, Nino]](AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex))

  }
}
