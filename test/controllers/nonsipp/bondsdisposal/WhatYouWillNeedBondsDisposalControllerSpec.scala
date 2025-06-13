/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.nonsipp.bondsdisposal

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ContentPageView

class WhatYouWillNeedBondsDisposalControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  "WhatYouWillNeedBondsDisposalController" - {

    lazy val viewModel = WhatYouWillNeedBondsDisposalController.viewModel(srn, schemeName)

    lazy val onPageLoad = routes.WhatYouWillNeedBondsDisposalController.onPageLoad(srn)
    lazy val onSubmit = routes.WhatYouWillNeedBondsDisposalController.onSubmit(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[ContentPageView]
      view(viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continue(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
