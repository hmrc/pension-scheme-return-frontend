$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

import pages.behaviours.PageBehaviours
import models._
$if(!index.empty)$
import config.RefinedTypes.$index$
import eu.timepit.refined.refineMV
$endif$

class $className;format="cap"$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineMV[$index$.Refined](1)

    beRetrievable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srn, index))

    beSettable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srn, index))

    beRemovable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srn, index))
    $else$
    beRetrievable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srn))

    beSettable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srn))

    beRemovable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srn))
    $endif$
  }
}