package controllers

import controllers.RemoveMemberDetailsController.{form, viewModel}
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{CheckMode, NameDOB, NormalMode}
import pages.{AccountingPeriodPage, MemberDetailsPage}
import views.html.YesNoPageView

import java.time.LocalDate

class RemoveMemberDetailsControllerSpec extends ControllerBaseSpec {
  private lazy val onPageLoad = routes.RemoveMemberDetailsController.onPageLoad(srn, refineMV(1), CheckMode)
  private lazy val onSubmit = routes.RemoveMemberDetailsController.onSubmit(srn, refineMV(1), CheckMode)

  val memberDetails = nameDobGen.sample.value

//  private val memberDetails = NameDOB("testFirstname", "testLastName", LocalDate.of(2020, 12, 12))

  private val userAnswers = defaultUserAnswers
    .set(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .success
    .value
    .set(MemberDetailsPage(srn, refineMV(2)), memberDetails)
    .success
    .value

  "RemoveMemberDetailsController" should {

    behave.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, refineMV(1), memberDetails, NormalMode)
      )
    })

    behave.like(redirectToPage(onPageLoad, controllers.routes.JourneyRecoveryController.onPageLoad()))

    behave.like(journeyRecoveryPage("onPageLoad", onPageLoad))

    behave.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))
    behave.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    behave.like(journeyRecoveryPage("onSubmit", onSubmit))

  }

}
