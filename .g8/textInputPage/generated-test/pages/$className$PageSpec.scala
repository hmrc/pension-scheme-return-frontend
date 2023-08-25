
package pages

import pages.behaviours.PageBehaviours
import models.Money
$if(!index.empty)$
import config.Refined.$index$
import eu.timepit.refined.refineMV
$endif$

class $className;format="cap"$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineMV[$index$.Refined](1)

    beRetrievable[String]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[String]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[String]($className;format="cap"$Page(srnGen.sample.value, index))
    $else$
    beRetrievable[String]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[String]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[String]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}