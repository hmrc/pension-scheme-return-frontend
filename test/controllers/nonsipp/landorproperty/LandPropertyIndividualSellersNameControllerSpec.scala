package controllers.nonsipp.landorproperty

import config.Refined.OneTo5000
import controllers.nonsipp.loansmadeoroutstanding.CompanyRecipientNameController.{form, viewModel}
import controllers.nonsipp.loansmadeoroutstanding.routes
import controllers.{routes, ControllerBaseSpec}
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.nonsipp.landorproperty.LandPropertyIndividualSellersNamePage
import pages.nonsipp.loansmadeoroutstanding.CompanyRecipientNamePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.TextInputView

import scala.concurrent.Future

class LandPropertyIndividualSellersNameControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  "IndividualSellersNameController" - {

    val populatedUserAnswers =
      defaultUserAnswers.set(LandPropertyIndividualSellersNamePage(srn, index), individualSellerName).get
    lazy val onPageLoad = routes.LandPropertyIndividualSellersNameController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.LandPropertyIndividualSellersNameController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, LandPropertyIndividualSellersNamePage(srn, index), individualSellerName) {
      implicit app => implicit request =>
        val preparedForm = form(injected[TextFormProvider]).fill(individualSellerName)
        injected[TextInputView].apply(preparedForm, viewModel(srn, index, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> individualSellerName))
    act.like(invalidForm(onSubmit, populatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
