package pages.nonsipp

import play.api.libs.json.{JsPath, __}

package object moneyborrowed {
  object Paths {
    val borrowingDetailsType: JsPath = __ \ "borrowingDetailsType"
  }
}
