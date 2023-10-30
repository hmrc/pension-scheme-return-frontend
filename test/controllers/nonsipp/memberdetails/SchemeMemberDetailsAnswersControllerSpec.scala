/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.nonsipp.memberdetails

import config.Refined.{Max300, OneTo300, OneTo5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.memberdetails.SchemeMemberDetailsAnswersController._
import eu.timepit.refined.refineMV
import models.{CheckOrChange, ConditionalYesNo, NameDOB}
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsPage}
import pages.nonsipp.moneyborrowed._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import uk.gov.hmrc.domain.Nino
import views.html.CheckYourAnswersView

class SchemeMemberDetailsAnswersControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private val index = refineMV[Max300.Refined](1)

  private def onPageLoad(checkOrChange: CheckOrChange) =
    routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, checkOrChange)

  private def onSubmit(checkOrChange: CheckOrChange) =
    routes.SchemeMemberDetailsAnswersController.onSubmit(srn, checkOrChange)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index), ConditionalYesNo.yes[String, Nino](nino))

  "MoneyBorrowedCYAController" - {

    List(CheckOrChange.Check, CheckOrChange.Change).foreach { checkOrChange =>
      act.like(
        renderView(onPageLoad(checkOrChange), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                memberDetails = memberDetails,
                hasNINO = ConditionalYesNo(Right(nino)),
                checkOrChange
              )
            )
          )
        }.withName(s"render correct ${checkOrChange.name} view")
      )

      act.like(
        redirectNextPage(onSubmit(checkOrChange))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .withName(s"redirect to next page when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(checkOrChange))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(checkOrChange))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${checkOrChange.name} mode")
      )
    }
  }
}
