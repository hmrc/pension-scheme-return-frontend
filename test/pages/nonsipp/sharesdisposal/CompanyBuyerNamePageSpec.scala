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

package pages.nonsipp.sharesdisposal

import config.RefinedTypes.{Max50, Max5000}
import eu.timepit.refined.refineMV
import pages.behaviours.PageBehaviours
class CompanyBuyerNamePageSpec extends PageBehaviours {
  private val srn = srnGen.sample.value

  "Share disposal - CompanyBuyerNamePage" - {

    val index = refineMV[Max5000.Refined](1)
    val disposalIndex = refineMV[Max50.Refined](1)

    beRetrievable[String](CompanyBuyerNamePage(srn, index, disposalIndex))

    beSettable[String](CompanyBuyerNamePage(srn, index, disposalIndex))

    beRemovable[String](CompanyBuyerNamePage(srn, index, disposalIndex))

    CompanyBuyerNamePage(srn, index, disposalIndex).toString mustBe "nameOfPurchaser"

  }

}
