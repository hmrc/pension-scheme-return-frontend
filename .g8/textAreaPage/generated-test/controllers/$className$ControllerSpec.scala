package controllers

import models.NormalMode
import forms.TextFormProvider
import views.html.TextAreaView
import pages.$className$Page
import $className;format="cap"$Controller._

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)

  "$className;format="cap"$Controller" - {

    act like renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextAreaView].apply(form(injected[TextFormProvider]), viewModel(srn, NormalMode))
    }

    act like renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), "test text") { implicit app => implicit request =>
      injected[TextAreaView].apply(form(injected[TextFormProvider]).fill("test text"), viewModel(srn, NormalMode))
    }

    act like redirectNextPage(onSubmit, "value" -> "test text")

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act like saveAndContinue(onSubmit, "value" -> "test text")

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act like invalidForm(onSubmit)
  }
}
