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

import utils.IntUtils.given
import models.{IdentitySubject, RecipientDetails}
import pages.behaviours.PageBehaviours

class OtherRecipientDetailsPageSpec extends PageBehaviours {
  private val index = 1
  private val srnSample = srnGen.sample.value

  "OtherRecipientDetailsPage" - {
    IdentitySubject.values.foreach { identitySubject =>
      s"for $identitySubject" - {

        beRetrievable[RecipientDetails](
          OtherRecipientDetailsPage(srnSample, index, identitySubject)
        )

        beSettable[RecipientDetails](
          OtherRecipientDetailsPage(srnSample, index, identitySubject)
        )

        beRemovable[RecipientDetails](
          OtherRecipientDetailsPage(srnSample, index, identitySubject)
        )
      }
    }
  }
}
