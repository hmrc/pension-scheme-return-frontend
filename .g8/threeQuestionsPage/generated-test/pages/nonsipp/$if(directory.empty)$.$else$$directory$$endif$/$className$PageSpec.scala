package pages

import pages.behaviours.PageBehaviours
import models._
$if(!index.empty)$
import config.Refined.$index$
import eu.timepit.refined.refineMV
$endif$

class $className$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineMV[$index$.Refined](1)

    beRetrievable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srn, index))

    beSettable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srn, index))

    beRemovable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srn, index))
    $else$
    beRetrievable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srn))

    beSettable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srn))

    beRemovable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srn))
    $endif$
  }
}
