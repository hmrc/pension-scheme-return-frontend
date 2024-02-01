
package pages

import pages.behaviours.PageBehaviours
import models.Money
$if(!index.empty)$
import config.Refined.$index$
import eu.timepit.refined.refineMV
$endif$

class $className;format="cap"$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineMV[$index$.Refined](1)

    beRetrievable[String]($className;format="cap"$Page(srn, index))

    beSettable[String]($className;format="cap"$Page(srn, index))

    beRemovable[String]($className;format="cap"$Page(srn, index))
    $else$
    beRetrievable[String]($className;format="cap"$Page(srn))

    beSettable[String]($className;format="cap"$Page(srn))

    beRemovable[String]($className;format="cap"$Page(srn))
    $endif$
  }
}