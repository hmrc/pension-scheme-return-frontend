package controllers

import models.NormalMode
import views.html.CheckYourAnswersView
import $className;format="cap"$Controller._

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)

  "$className;format="cap"$Controller" - {

    act like renderView(onPageLoad) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(viewModel(srn, NormalMode))
    }

    act like redirectNextPage(onSubmit)

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
