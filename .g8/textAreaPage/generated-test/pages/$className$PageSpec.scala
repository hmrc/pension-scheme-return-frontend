package pages

import pages.behaviours.PageBehaviours

class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    beRetrievable[String]($className$Page(srnGen.sample.value))

    beSettable[String]($className$Page(srnGen.sample.value))

    beRemovable[String]($className$Page(srnGen.sample.value))
  }
}
