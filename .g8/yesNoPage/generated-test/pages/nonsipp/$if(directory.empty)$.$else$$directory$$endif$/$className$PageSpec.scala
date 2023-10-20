$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

import pages.behaviours.PageBehaviours
import models.Money
$if(!index.empty)$
import config.Refined._
import eu.timepit.refined.refineMV
$endif$

class $className;format="cap"$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineMV[$index$.Refined](1)

    beRetrievable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index))
    $else$
    beRetrievable[Boolean]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[Boolean]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[Boolean]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}