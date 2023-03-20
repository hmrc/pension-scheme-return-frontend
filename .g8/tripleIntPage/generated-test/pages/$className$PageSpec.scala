package pages

import pages.behaviours.PageBehaviours

class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    beRetrievable[(Int, Int, Int)]($className$Page(srnGen.sample.value))

    beSettable[(Int, Int, Int)]($className$Page(srnGen.sample.value))

    beRemovable[(Int, Int, Int)]($className$Page(srnGen.sample.value))
  }
}
