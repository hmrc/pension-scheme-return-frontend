package pages.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import eu.timepit.refined.refineMV
import pages.behaviours.PageBehaviours

class DisposalIndependentValuationPageSpec extends PageBehaviours {

  "DisposalIndependentValuationPage" - {

    val index = refineMV[Max5000.Refined](1)
    val disposalIndex = refineMV[Max50.Refined](1)

    beRetrievable[Boolean](DisposalIndependentValuationPage(srnGen.sample.value, index, disposalIndex))

    beSettable[Boolean](DisposalIndependentValuationPage(srnGen.sample.value, index, disposalIndex))

    beRemovable[Boolean](DisposalIndependentValuationPage(srnGen.sample.value, index, disposalIndex))
  }

}