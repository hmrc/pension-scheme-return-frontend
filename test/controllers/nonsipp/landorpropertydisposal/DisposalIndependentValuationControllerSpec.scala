package controllers.nonsipp.landorpropertydisposal


import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import controllers.nonsipp.landorpropertydisposal.DisposalIndependentValuationController.{form, viewModel}
import pages.nonsipp.landorpropertydisposal.DisposalIndependentValuationPage
import views.html.YesNoPageView

class DisposalIndependentValuationControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    controllers.nonsipp.landorpropertydisposal.routes.DisposalIndependentValuationController
      .onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = controllers.nonsipp.landorpropertydisposal.routes.DisposalIndependentValuationController
    .onSubmit(srn, index, disposalIndex, NormalMode)

  private val userAnswers = userAnswersWithAddress(srn, index)

  "DisposalIndependentValuationController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            index,
            disposalIndex,
            NormalMode,
            address.addressLine1
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, DisposalIndependentValuationPage(srn, index, disposalIndex), true, userAnswers) {
        implicit app => implicit request =>
          injected[YesNoPageView]
            .apply(
              form(injected[YesNoPageFormProvider]).fill(true),
              viewModel(srn, index, disposalIndex, NormalMode, address.addressLine1)
            )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}