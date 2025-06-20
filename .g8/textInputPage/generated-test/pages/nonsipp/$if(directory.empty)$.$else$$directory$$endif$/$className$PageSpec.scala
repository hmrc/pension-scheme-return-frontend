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