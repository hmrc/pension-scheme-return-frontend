package pages

import pages.behaviours.PageBehaviours

class OutstandingArrearsOnLoanPageSpec extends PageBehaviours {

  "OutstandingArrearsOnLoanPage" - {

    beRetrievable[Boolean](OutstandingArrearsOnLoanPage(srnGen.sample.value))

    beSettable[Boolean](OutstandingArrearsOnLoanPage(srnGen.sample.value))

    beRemovable[Boolean](OutstandingArrearsOnLoanPage(srnGen.sample.value))
  }
}
