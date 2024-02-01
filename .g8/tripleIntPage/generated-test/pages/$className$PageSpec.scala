package pages

import pages.behaviours.PageBehaviours

class $className$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    beRetrievable[(Int, Int, Int)]($className$Page(srn))

    beSettable[(Int, Int, Int)]($className$Page(srn))

    beRemovable[(Int, Int, Int)]($className$Page(srn))
  }
}
