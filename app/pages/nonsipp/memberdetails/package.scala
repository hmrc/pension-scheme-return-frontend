package pages.nonsipp

import play.api.libs.json.__

package object memberdetails {
  object Paths {
    val personalDetails = __ \ "membersPayments" \ "memberDetails" \ "personalDetails"
  }
}
