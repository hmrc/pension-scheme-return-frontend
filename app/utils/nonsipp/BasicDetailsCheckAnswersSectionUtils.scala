/*
 * Copyright 2025 HM Revenue & Customs
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

package utils.nonsipp

import services.SchemeDateService
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import viewmodels.implicits._
import play.api.mvc.{AnyContent, Result}
import utils.ListUtils.ListOps
import cats.implicits.toShow
import models.DateRange._
import pages.nonsipp.WhichTaxYearPage
import play.api.i18n._
import models.requests.DataRequest
import config.RefinedTypes.Max3
import controllers.PsrControllerHelpers
import cats.data.NonEmptyList
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{CheckMode, _}
import viewmodels.DisplayMessage._
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersSection}

import java.time.LocalDate

object BasicDetailsCheckAnswersSectionUtils extends PsrControllerHelpers {

  def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }

  def basicDetailsSections(srn: Srn, mode: Mode, schemeDateService: SchemeDateService)(using
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, List[CheckYourAnswersSection]] = {
    for {
      schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
      activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
      whyNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
      whichTaxYearPage = request.userAnswers.get(WhichTaxYearPage(srn))
      //      compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      taxYearOrAccountingPeriods <- schemeDateService.taxYearOrAccountingPeriods(srn).getOrRecoverJourney
      schemeDetails = request.schemeDetails
      pensionSchemeId = request.pensionSchemeId
      isPSP = pensionSchemeId.isPSP
      schemeAdminName <- loggedInUserNameOrRedirect
    } yield List(
      CheckYourAnswersSection(
        Some(Heading2.medium("basicDetailsCheckYourAnswersController.schemeDetails.heading")),
        List(
          CheckYourAnswersRowViewModel(
            "basicDetailsCheckYourAnswersController.schemeDetails.schemeName",
            schemeDetails.schemeName
          ).withOneHalfWidth(),
          CheckYourAnswersRowViewModel(
            "basicDetailsCheckYourAnswersController.schemeDetails.pstr",
            schemeDetails.pstr
          ).withOneHalfWidth(),
          CheckYourAnswersRowViewModel(
            if (isPSP) {
              "basicDetailsCheckYourAnswersController.schemeDetails.schemePractitionerName"
            } else {
              "basicDetailsCheckYourAnswersController.schemeDetails.schemeAdminName"
            },
            schemeAdminName
          ).withOneHalfWidth(),
          CheckYourAnswersRowViewModel(
            if (isPSP) {
              "basicDetailsCheckYourAnswersController.schemeDetails.practitionerId"
            } else {
              "basicDetailsCheckYourAnswersController.schemeDetails.adminId"
            },
            pensionSchemeId.value
          ).withOneHalfWidth()
        ) ++
          (taxYearOrAccountingPeriods match {
            case Left(taxYear) =>
              List(
                CheckYourAnswersRowViewModel(
                  "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                  taxYear.show
                ).withChangeAction(
                  controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url,
                  hidden = "basicDetailsCheckYourAnswersController.schemeDetails.taxYear.hidden"
                ).withOneHalfWidth()
              )
            case Right(accountingPeriods) =>
              List(
                CheckYourAnswersRowViewModel(
                  "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                  whichTaxYearPage.get.show
                ).withChangeAction(
                  controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url,
                  hidden = "basicDetailsCheckYourAnswersController.schemeDetails.taxYear.hidden"
                ).withOneHalfWidth()
              ) ++
                List(
                  CheckYourAnswersRowViewModel(
                    Message("basicDetailsCheckYourAnswersController.schemeDetails.accountingPeriod"),
                    accountingPeriods.map(_._1.show).toList.mkString("\n")
                  ).withChangeAction(
                    controllers.nonsipp.accountingperiod.routes.AccountingPeriodListController
                      .onPageLoad(srn, CheckMode)
                      .url,
                    hidden = "basicDetailsCheckYourAnswersController.schemeDetails.accountingPeriod.hidden"
                  ).withOneHalfWidth()
                )
          })
          :+ CheckYourAnswersRowViewModel(
            Message("basicDetailsCheckYourAnswersController.schemeDetails.bankAccount"),
            if (activeBankAccount: Boolean) "site.yes" else "site.no"
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, CheckMode).url,
            hidden = "basicDetailsCheckYourAnswersController.schemeDetails.bankAccount.hidden"
          ).withOneHalfWidth()
          :?+ whyNoBankAccount.map(reason =>
            CheckYourAnswersRowViewModel(
              Message("basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount"),
              reason
            ).withChangeAction(
              controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, CheckMode).url,
              hidden = "basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount.hidden"
            ).withOneHalfWidth()
          )
      ),
      CheckYourAnswersSection(
        Some(Heading2.medium("basicDetailsCheckYourAnswersController.memberDetails.heading")),
        List(
          CheckYourAnswersRowViewModel(
            Message(
              "basicDetailsCheckYourAnswersController.memberDetails.activeMembers",
              schemeDetails.schemeName,
              taxEndDate(taxYearOrAccountingPeriods).show
            ),
            schemeMemberNumbers.noOfActiveMembers.toString
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.HowManyMembersController
              .onPageLoad(srn, mode)
              .url + "#activeMembers",
            hidden = Message(
              "basicDetailsCheckYourAnswersController.memberDetails.activeMembers.hidden",
              taxEndDate(taxYearOrAccountingPeriods).show
            )
          ).withOneHalfWidth(),
          CheckYourAnswersRowViewModel(
            Message(
              "basicDetailsCheckYourAnswersController.memberDetails.deferredMembers",
              schemeDetails.schemeName,
              taxEndDate(taxYearOrAccountingPeriods).show
            ),
            schemeMemberNumbers.noOfDeferredMembers.toString
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.HowManyMembersController
              .onPageLoad(srn, mode)
              .url + "#deferredMembers",
            hidden = Message(
              "basicDetailsCheckYourAnswersController.memberDetails.deferredMembers.hidden",
              taxEndDate(taxYearOrAccountingPeriods).show
            )
          ).withOneHalfWidth(),
          CheckYourAnswersRowViewModel(
            Message(
              "basicDetailsCheckYourAnswersController.memberDetails.pensionerMembers",
              schemeDetails.schemeName,
              taxEndDate(taxYearOrAccountingPeriods).show
            ),
            schemeMemberNumbers.noOfPensionerMembers.toString
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.HowManyMembersController
              .onPageLoad(srn, mode)
              .url + "#pensionerMembers",
            hidden = Message(
              "basicDetailsCheckYourAnswersController.memberDetails.pensionerMembers.hidden",
              taxEndDate(taxYearOrAccountingPeriods).show
            )
          ).withOneHalfWidth()
        )
      )
    )
  }

}
