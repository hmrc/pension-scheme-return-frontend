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

package pages

import eu.timepit.refined.refineMV
import models.DateRange
import pages.behaviours.PageBehaviours
import uk.gov.hmrc.domain.Nino

class MemberDetailsNinoPageSpec extends PageBehaviours {

  "MemberDetailsNinoPage" - {

    val srn = srnGen.sample.value

    beRetrievable[Nino](MemberDetailsNinoPage(srn, refineMV(1)))

    beSettable[Nino](MemberDetailsNinoPage(srn, refineMV(1)))

    beRemovable[Nino](MemberDetailsNinoPage(srn, refineMV(1)))
  }
}
