package controllers.nonsipp.memberpayments

import controllers.ControllerBaseSpec
import views.html.ContentPageView

class WYWNeedReceivedTransferControllerSpec extends ControllerBaseSpec {

  "WYWNeedReceivedTransferController" - {

    lazy val viewModel = WYWNeedReceivedTransferController.viewModel(srn, schemeName)

    lazy val onPageLoad = routes.WYWNeedReceivedTransferController.onPageLoad(srn)
    lazy val onSubmit = routes.WYWNeedReceivedTransferController.onSubmit(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[ContentPageView]
      view(viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continue(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
