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

$! Generic !$
$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

$if(directory.empty)$
import pages.nonsipp.$className$Page
$else$
import pages.nonsipp.$directory$.$className$Page
$endif$

$if(!index.empty)$
import config.RefinedTypes._
import utils.IntUtils.given
$endif$

import pages.behaviours.PageBehaviours
$! Generic end !$

$! Generic (change page type) !$
class $className$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    $if(!index.empty)$
    val index = 1
    $if(!secondaryIndex.empty)$
    val secondaryIndex = 1

    beRetrievable[String]($className;format="cap"$Page(srn, index, secondaryIndex))

    beSettable[String]($className;format="cap"$Page(srn, index, secondaryIndex))

    beRemovable[String]($className;format="cap"$Page(srn, index, secondaryIndex))
    $else$
    beRetrievable[String]($className;format="cap"$Page(srn, index))

    beSettable[String]($className;format="cap"$Page(srn, index))

    beRemovable[String]($className;format="cap"$Page(srn, index))
    $endif$
    $else$
    beRetrievable[String]($className;format="cap"$Page(srn))

    beSettable[String]($className;format="cap"$Page(srn))

    beRemovable[String]($className;format="cap"$Page(srn))
    $endif$
  }
}
$! Generic end !$
