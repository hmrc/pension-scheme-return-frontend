package pages.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import eu.timepit.refined.refineMV
import pages.behaviours.PageBehaviours

class PartnershipBuyerUtrPageSpec extends PageBehaviours {

  "PartnershipBuyerUtrPage" - {

    val index = refineMV[Max5000.Refined](1)
    val disposalIndex = refineMV[Max50.Refined](1)

    beRetrievable[String](CompanyBuyerNamePage(srnGen.sample.value, index, disposalIndex))

    beSettable[String](CompanyBuyerNamePage(srnGen.sample.value, index, disposalIndex))

    beRemovable[String](CompanyBuyerNamePage(srnGen.sample.value, index, disposalIndex))
  }

}