package pages

import pages.behaviours.PageBehaviours

class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    beRetrievable[Boolean]($className$Page(srnGen.sample.value))

    beSettable[Boolean]($className$Page(srnGen.sample.value))

    beRemovable[Boolean]($className$Page(srnGen.sample.value))
  }
}
