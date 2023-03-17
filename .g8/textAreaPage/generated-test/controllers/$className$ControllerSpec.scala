package controllers

import models.NormalMode
import forms.TextFormProvider
import views.html.TextAreaView
import pages.$className$Page
import $className;format="cap"$Controller._

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)

  "$className;format="cap"$Controller" should {

    behave like renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextAreaView].apply(form(injected[TextFormProvider]), viewModel(srn, NormalMode))
    }

    behave like renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), "test text") { implicit app => implicit request =>
      injected[TextAreaView].apply(form(injected[TextFormProvider]).fill("test text"), viewModel(srn, NormalMode))
    }

    behave like redirectNextPage(onSubmit, "value" -> "test text")

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)

    behave like saveAndContinue(onSubmit, "value" -> "test text")

    behave like journeyRecoveryPage("onSubmit", onSubmit)

    behave like invalidForm(onSubmit)
  }
}
