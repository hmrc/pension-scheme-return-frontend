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

package controllers.nonsipp.landorproperty

import services.SchemeDateService
import controllers.nonsipp.landorproperty.LandOrPropertyLeaseDetailsController._
import play.api.inject.guice.GuiceableModule
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.MultipleQuestionView
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandOrPropertyLeaseDetailsPage}
import eu.timepit.refined.refineMV
import models.{DateRange, NormalMode}

class LandOrPropertyLeaseDetailsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private lazy val onPageLoad = routes.LandOrPropertyLeaseDetailsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertyLeaseDetailsController.onSubmit(srn, index, NormalMode)

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  val taxYear: Some[Left[DateRange, Nothing]] = Some(Left(dateRange))

  private val dateTooEarlyForm = List(
    "value.1" -> "test",
    "value.2" -> "1",
    "value.3.day" -> "31",
    "value.3.month" -> "12",
    "value.3.year" -> "1899"
  )

  private val userAnswersWithAddress =
    defaultUserAnswers.unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)

  override def beforeEach(): Unit =
    reset(mockSchemeDateService)

  "LandOrPropertyLeaseDetailsController" - {

    act.like(renderView(onPageLoad, userAnswersWithAddress) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(
        viewModel(srn, index, address.addressLine1, form(localDate), NormalMode)
      )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(
      renderPrePopView(
        onPageLoad,
        LandOrPropertyLeaseDetailsPage(srn, index),
        (leaseName, money, localDate),
        userAnswersWithAddress
      ) { implicit app => implicit request =>
        injected[MultipleQuestionView].apply(
          viewModel(srn, index, address.addressLine1, form(localDate).fill((leaseName, money, localDate)), NormalMode)
        )
      }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      redirectNextPage(
        onSubmit,
        userAnswersWithAddress,
        "value.1" -> "test",
        "value.2" -> "1",
        "value.3.day" -> "1",
        "value.3.month" -> "4",
        "value.3.year" -> "2021"
      ).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        userAnswersWithAddress,
        "value.1" -> "test",
        "value.2" -> "1",
        "value.3.day" -> "1",
        "value.3.month" -> "4",
        "value.3.year" -> "2021"
      ).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      invalidForm(onSubmit, userAnswersWithAddress)
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      invalidForm(onSubmit, userAnswersWithAddress, dateTooEarlyForm: _*)
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )
  }
}
