package pages

import play.api.libs.json.JsPath
import models.SchemeId.Srn
import pages.Page

case class $className;format="cap"$Page(srn: Srn) extends Page
