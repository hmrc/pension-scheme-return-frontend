package pages

import pages.behaviours.PageBehaviours

class $className$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    beRetrievable[???]($className$Page(srn))

    beSettable[???]($className$Page(srn))

    beRemovable[???]($className$Page(srn))
  }
}
