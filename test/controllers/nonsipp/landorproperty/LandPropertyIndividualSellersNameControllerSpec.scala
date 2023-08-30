package controllers.nonsipp.landorproperty

import config.Refined.{Max5000, OneTo5000}
import LandPropertyIndividualSellersNameController.{form, viewModel}
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.landorproperty.LandPropertyIndividualSellersNamePage
import views.html.TextInputView

class LandPropertyIndividualSellersNameControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private lazy val onPageLoad = routes.LandPropertyIndividualSellersNameController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandPropertyIndividualSellersNameController.onSubmit(srn, index, NormalMode)

  "IndividualSellersNameController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, LandPropertyIndividualSellersNamePage(srn, index), "test") {
      implicit app => implicit request =>
        injected[TextInputView].apply(form(injected[TextFormProvider]).fill("test"), viewModel(srn, index, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "test"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "test"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
