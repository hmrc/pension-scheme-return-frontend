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

package pages.nonsipp.common

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn, IdentitySubject}
import pages.behaviours.PageBehaviours

class CompanyRecipientCrnPageSpec extends PageBehaviours {

  private val srnSample = srnGen.sample.value

  "CompanyRecipientCrnPage" - {

    val index = refineMV[OneTo5000](1)

    IdentitySubject.values.foreach { identitySubject =>
      s"for $identitySubject" - {

        beRetrievable[ConditionalYesNo[String, Crn]](
          CompanyRecipientCrnPage(srnSample, index, identitySubject)
        )

        beSettable[ConditionalYesNo[String, Crn]](
          CompanyRecipientCrnPage(srnSample, index, identitySubject)
        )

        beRemovable[ConditionalYesNo[String, Crn]](
          CompanyRecipientCrnPage(srnSample, index, identitySubject)
        )
      }
    }
  }
}
