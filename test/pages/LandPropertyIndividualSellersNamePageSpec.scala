package pages

import config.Refined.Max5000
import eu.timepit.refined.refineMV
import pages.behaviours.PageBehaviours
import pages.nonsipp.landorproperty.LandPropertyIndividualSellersNamePage

class LandPropertyIndividualSellersNamePageSpec extends PageBehaviours {

  "LandPropertyIndividualSellersNamePage" - {

    val srn = srnGen.sample.value
    val index = refineMV[Max5000.Refined](1)

    beRetrievable[String](LandPropertyIndividualSellersNamePage(srn, index))

    beSettable[String](LandPropertyIndividualSellersNamePage(srn, index))

    beRemovable[String](LandPropertyIndividualSellersNamePage(srn, index))
  }
}
