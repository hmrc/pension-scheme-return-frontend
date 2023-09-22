package controllers.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorpropertydisposal.PartnershipBuyerUtrController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Utr, NormalMode}
import pages.nonsipp.landorpropertydisposal.PartnershipBuyerUtrPage
import views.html.ConditionalYesNoPageView

class PartnershipBuyerUtrControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.PartnershipBuyerUtrController
      .onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.PartnershipBuyerUtrController
      .onSubmit(srn, index, disposalIndex, NormalMode)

  val userAnswersCompanyName =
    defaultUserAnswers.unsafeSet(CompanyBuyerNamePage(srn, index, disposalIndex), companyName) //TODO to change when the PartnershipNamePage is ready

  val conditionalNo: ConditionalYesNo[String, Utr] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)

  "CompanyBuyerCrnController" - {

    act.like(renderView(onPageLoad, userAnswersCompanyName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, disposalIndex, NormalMode, companyName)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        PartnershipBuyerUtrPage(srn, index, disposalIndex),
        conditionalNo,
        userAnswersCompanyName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, index, disposalIndex, NormalMode, companyName)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> utr.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> utr.value))

    act.like(invalidForm(onSubmit, userAnswersCompanyName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}