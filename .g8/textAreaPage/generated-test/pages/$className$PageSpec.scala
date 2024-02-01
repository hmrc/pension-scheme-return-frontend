package pages

import pages.behaviours.PageBehaviours

class $className$PageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "$className$Page" - {

    beRetrievable[String]($className$Page(srn))

    beSettable[String]($className$Page(srn))

    beRemovable[String]($className$Page(srn))
  }
}
