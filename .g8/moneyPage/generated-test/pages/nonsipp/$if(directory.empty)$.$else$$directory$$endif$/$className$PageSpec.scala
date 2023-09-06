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

$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

import pages.behaviours.PageBehaviours
import models.Money
$if(!index.empty)$
import config.Refined.$index$
import eu.timepit.refined.refineMV
$endif$

class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineMV[$index$.Refined](1)

    beRetrievable[Money]($className$Page(srnGen.sample.value, index))

    beSettable[Money]($className$Page(srnGen.sample.value, index))

    beRemovable[Money]($className$Page(srnGen.sample.value, index))
    $else$
    beRetrievable[Money]($className$Page(srnGen.sample.value))

    beSettable[Money]($className$Page(srnGen.sample.value))

    beRemovable[Money]($className$Page(srnGen.sample.value))
    $endif$
  }
}
