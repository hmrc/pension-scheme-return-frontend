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

package controllers.nonsipp

import cats.data.NonEmptyList
import cats.implicits.toShow
import config.Refined.{Max3, OneToThree}
import eu.timepit.refined.refineV
import controllers.actions._
import controllers.nonsipp.BasicDetailsCheckYourAnswersController._
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{CheckMode, DateRange, Mode, SchemeDetails, SchemeMemberNumbers}
import navigation.Navigator
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import pages.nonsipp.{BasicDetailsCheckYourAnswersPage, WhichTaxYearPage}
import play.api.i18n._
import play.api.libs.json.Reads
import play.api.mvc._
import queries.Gettable
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import utils.ListUtils.ListOps
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}

class BasicDetailsCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(periods) =>
        (
          for {
            schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
            activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
            whyNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
            whichTaxYearPage = request.userAnswers.get(WhichTaxYearPage(srn))
            userName <- loggedInUserNameOrRedirect
          } yield Ok(
            view(
              viewModel(
                srn,
                mode,
                schemeMemberNumbers,
                activeBankAccount,
                whyNoBankAccount,
                whichTaxYearPage,
                periods,
                userName,
                request.schemeDetails,
                request.pensionSchemeId.value
              )
            )
          )
        ).merge
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(BasicDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
  }

  private def loggedInUserNameOrRedirect(implicit request: DataRequest[_]): Either[Result, String] =
    request.minimalDetails.individualDetails match {
      case Some(individual) => Right(individual.fullName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  private def requiredPage[A: Reads](page: Gettable[A])(implicit request: DataRequest[_]): Either[Result, A] =
    request.userAnswers.get(page) match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
}

object BasicDetailsCheckYourAnswersController {
  def viewModel(
    srn: Srn,
    mode: Mode,
    schemeMemberNumbers: SchemeMemberNumbers,
    activeBankAccount: Boolean,
    whyNoBankAccount: Option[String],
    whichTaxYearPage: Option[DateRange],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeAdminName: String,
    schemeDetails: SchemeDetails,
    pensionSchemeId: String
  )(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = "checkYourAnswers.title",
      heading = "checkYourAnswers.heading",
      description = Some(ParagraphMessage("basicDetailsCheckYourAnswers.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          mode,
          activeBankAccount,
          whyNoBankAccount,
          whichTaxYearPage,
          taxYearOrAccountingPeriods,
          schemeMemberNumbers,
          schemeAdminName,
          schemeDetails,
          pensionSchemeId
        )
      ).withMarginBottom(9),
      refresh = None,
      buttonText = "site.saveAndContinue",
      onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, mode)
    )

  private def sections(
    srn: Srn,
    mode: Mode,
    activeBankAccount: Boolean,
    whyNoBankAccount: Option[String],
    whichTaxYearPage: Option[DateRange],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeMemberNumbers: SchemeMemberNumbers,
    schemeAdminName: String,
    schemeDetails: SchemeDetails,
    pensionSchemeId: String
  )(
    implicit messages: Messages
  ): List[CheckYourAnswersSection] = List(
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
          "basicDetailsCheckYourAnswersController.schemeDetails.schemeAdminName",
          schemeAdminName
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          "basicDetailsCheckYourAnswersController.schemeDetails.adminId",
          pensionSchemeId
        ).withOneHalfWidth()
      ) ++
        (taxYearOrAccountingPeriods match {
          case Left(taxYear) =>
            List(
              CheckYourAnswersRowViewModel(
                "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                taxYear.show
              ).withOneHalfWidth()
            )
          case Right(accountingPeriods) =>
            List(
              CheckYourAnswersRowViewModel(
                "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                whichTaxYearPage.get.show
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
                  )
                  .withOneHalfWidth()
              )
        })
        :+ CheckYourAnswersRowViewModel(
          Message("basicDetailsCheckYourAnswersController.schemeDetails.bankAccount", schemeDetails.schemeName),
          if (activeBankAccount: Boolean) "site.yes" else "site.no"
        ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, CheckMode).url,
            hidden = "basicDetailsCheckYourAnswersController.schemeDetails.bankAccount.hidden"
          )
          .withOneHalfWidth()
        :?+ whyNoBankAccount.map(
          reason =>
            CheckYourAnswersRowViewModel(
              Message(
                "basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount",
                schemeDetails.schemeName
              ),
              reason
            ).withChangeAction(
                controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, CheckMode).url,
                hidden = "basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount.hidden"
              )
              .withOneHalfWidth()
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
            controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, mode).url,
            hidden = Message(
              "basicDetailsCheckYourAnswersController.memberDetails.activeMembers.hidden",
              taxEndDate(taxYearOrAccountingPeriods).show
            )
          )
          .withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          Message(
            "basicDetailsCheckYourAnswersController.memberDetails.deferredMembers",
            schemeDetails.schemeName,
            taxEndDate(taxYearOrAccountingPeriods).show
          ),
          schemeMemberNumbers.noOfDeferredMembers.toString
        ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, mode).url,
            hidden = Message(
              "basicDetailsCheckYourAnswersController.memberDetails.deferredMembers.hidden",
              taxEndDate(taxYearOrAccountingPeriods).show
            )
          )
          .withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          Message(
            "basicDetailsCheckYourAnswersController.memberDetails.pensionerMembers",
            schemeDetails.schemeName,
            taxEndDate(taxYearOrAccountingPeriods).show
          ),
          schemeMemberNumbers.noOfPensionerMembers.toString
        ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, mode).url,
            hidden = Message(
              "basicDetailsCheckYourAnswersController.memberDetails.pensionerMembers.hidden",
              taxEndDate(taxYearOrAccountingPeriods).show
            )
          )
          .withOneHalfWidth()
      )
    )
  )

  private def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }
}
