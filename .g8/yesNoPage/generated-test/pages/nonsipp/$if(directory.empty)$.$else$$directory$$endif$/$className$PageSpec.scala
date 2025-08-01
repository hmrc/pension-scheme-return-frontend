$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

import pages.behaviours.PageBehaviours
import models.Money
$if(!index.empty)$
import config.RefinedTypes._
import utils.IntUtils.given
$endif$

$! Generic (change page type) !$
class $className$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    $if(!index.empty)$
    val index = 1
    $if(!secondaryIndex.empty)$
    val secondaryIndex = 1

    beRetrievable[Boolean]($className;format="cap"$Page(srn, index, secondaryIndex))

    beSettable[Boolean]($className;format="cap"$Page(srn, index, secondaryIndex))

    beRemovable[Boolean]($className;format="cap"$Page(srn, index, secondaryIndex))
    $else$
    beRetrievable[Boolean]($className;format="cap"$Page(srn, index))

    beSettable[Boolean]($className;format="cap"$Page(srn, index))

    beRemovable[Boolean]($className;format="cap"$Page(srn, index))
    $endif$
    $else$
    beRetrievable[Boolean]($className;format="cap"$Page(srn))

    beSettable[Boolean]($className;format="cap"$Page(srn))

    beRemovable[Boolean]($className;format="cap"$Page(srn))
    $endif$
  }
}
$! Generic end !$