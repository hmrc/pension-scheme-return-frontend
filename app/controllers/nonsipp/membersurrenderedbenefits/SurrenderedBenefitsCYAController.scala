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

package controllers.nonsipp.membersurrenderedbenefits

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import viewmodels.implicits._
import play.api.mvc._
import utils.IntUtils.{toInt, toRefined300}
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import pages.nonsipp.membersurrenderedbenefits._
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsCYAController._
import models.requests.DataRequest
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.MessagesApi
import viewmodels.Margin
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class SurrenderedBenefitsCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadCommon(srn: SchemeId.Srn, index: Max300, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    (
      for {
        memberDetails <- request.userAnswers
          .get(MemberDetailsPage(srn, index))
          .getOrRecoverJourney
        surrenderedBenefitsAmount <- request.userAnswers
          .get(SurrenderedBenefitsAmountPage(srn, index))
          .getOrRecoverJourney
        whenSurrenderedBenefits <- request.userAnswers
          .get(WhenDidMemberSurrenderBenefitsPage(srn, index))
          .getOrRecoverJourney
        whySurrenderedBenefits <- request.userAnswers
          .get(WhyDidMemberSurrenderBenefitsPage(srn, index))
          .getOrRecoverJourney
      } yield Ok(
        view(
          viewModel(
            srn,
            index,
            memberDetails.fullName,
            surrenderedBenefitsAmount,
            whenSurrenderedBenefits,
            whySurrenderedBenefits,
            mode,
            viewOnlyUpdated = false,
            optYear = request.year,
            optCurrentVersion = request.currentVersion,
            optPreviousVersion = request.previousVersion
          )
        )
      )
    ).merge

  def onSubmit(srn: Srn, memberIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val surrenderedBenefitsChanged =
        request.userAnswers.changed(_.buildSurrenderedBenefits(srn, memberIndex))

      for {
        updatedUserAnswers <- request.userAnswers
          .set(SurrenderedBenefitsCompletedPage(srn, memberIndex), SectionCompleted)
          .setWhen(surrenderedBenefitsChanged)(MemberStatus(srn, memberIndex), MemberState.Changed)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall = controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
            .onPageLoad(srn, memberIndex, mode)
        )
      } yield submissionResult.getOrRecoverJourney(_ =>
        Redirect(
          navigator.nextPage(SurrenderedBenefitsCYAPage(srn, memberIndex), mode, request.userAnswers)
        )
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object SurrenderedBenefitsCYAController {
  def viewModel(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    surrenderedBenefitsAmount: Money,
    whenSurrenderedBenefits: LocalDate,
    whySurrenderedBenefits: String,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "surrenderedBenefits.cya.title",
        check = "surrenderedBenefits.change.title",
        viewOnly = "surrenderedBenefits.cya.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "surrenderedBenefits.cya.heading",
        check = Message("surrenderedBenefits.change.heading", memberName),
        viewOnly = Message("surrenderedBenefits.cya.viewOnly.heading", memberName)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections = rows(
          srn,
          memberIndex,
          memberName,
          surrenderedBenefitsAmount,
          whenSurrenderedBenefits,
          whySurrenderedBenefits,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ).withMarginBottom(Margin.Fixed60Bottom),
      refresh = None,
      buttonText = mode.fold(
        normal = "site.saveAndContinue",
        check = "site.continue",
        viewOnly = "site.continue"
      ),
      onSubmit = routes.SurrenderedBenefitsCYAController.onSubmit(srn, memberIndex, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "surrenderedBenefits.cya.viewOnly.title",
            heading = Message("surrenderedBenefits.cya.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                routes.SurrenderedBenefitsCYAController.onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                routes.SurrenderedBenefitsCYAController.onSubmit(srn, memberIndex, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def rows(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    surrenderedBenefitsAmount: Money,
    whenSurrenderedBenefits: LocalDate,
    whySurrenderedBenefits: String,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("surrenderedBenefits.cya.section.memberName"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("surrenderedBenefits.cya.section.amount", memberName),
            Message(s"£${surrenderedBenefitsAmount.displayAs}")
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsAmountController
                .onSubmit(srn, memberIndex, mode)
                .url
            ).withVisuallyHiddenContent(Message("surrenderedBenefits.cya.section.amount.hidden", memberName))
          ),
          CheckYourAnswersRowViewModel(
            Message("surrenderedBenefits.cya.section.date", memberName),
            whenSurrenderedBenefits.show
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membersurrenderedbenefits.routes.WhenDidMemberSurrenderBenefitsController
                .onSubmit(srn, memberIndex, mode)
                .url
            ).withVisuallyHiddenContent(Message("surrenderedBenefits.cya.section.date.hidden", memberName))
          ),
          CheckYourAnswersRowViewModel(
            Message("surrenderedBenefits.cya.section.reason", memberName),
            whySurrenderedBenefits.show
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membersurrenderedbenefits.routes.WhyDidMemberSurrenderBenefitsController
                .onSubmit(srn, memberIndex, mode)
                .url
            ).withVisuallyHiddenContent(Message("surrenderedBenefits.cya.section.reason.hidden", memberName))
          )
        )
      )
    )
}
