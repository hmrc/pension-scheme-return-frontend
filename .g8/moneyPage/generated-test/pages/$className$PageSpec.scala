package pages

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
