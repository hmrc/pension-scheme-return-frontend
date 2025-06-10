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

package pages

import pages.nonsipp.memberdetails.MemberPsrVersionPage
import pages.nonsipp.memberreceivedpcls.Paths
import utils.IntUtils.given
import models.SchemeId
import pages.behaviours.PageBehaviours

class MemberPsrVersionPageSpec extends PageBehaviours {

  private val srn: SchemeId.Srn = srnGen.sample.value
  private val memberIndex = 1

  "MemberPsrVersionPage" - {

    beRetrievable[String](MemberPsrVersionPage(srn, memberIndex))

    beSettable[String](MemberPsrVersionPage(srn, memberIndex))

    beRemovable[String](MemberPsrVersionPage(srn, memberIndex))

    "must return the correct JsPath" in {
      MemberPsrVersionPage.all(srn).path mustEqual Paths.memberDetails \ "memberPSRVersion"
    }
  }

}
