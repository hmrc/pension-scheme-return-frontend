package pages

import models.$className$
import pages.behaviours.PageBehaviours

class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    beRetrievable[$className$]($className$Page(srnGen.sample.value))

    beSettable[$className$]($className$Page(srnGen.sample.value))

    beRemovable[$className$]($className$Page(srnGen.sample.value))
  }
}
