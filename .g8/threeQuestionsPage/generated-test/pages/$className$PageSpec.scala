package pages

import pages.behaviours.PageBehaviours
import models._
$if(!index.empty)$
import config.Refined.$index$
import eu.timepit.refined.refineMV
$endif$

class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineMV[$index$.Refined](1)

    beRetrievable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value, index))
    $else$
    beRetrievable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}
