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

package controllers.nonsipp.membersurrenderedbenefits

import cats.implicits.toShow
import config.Refined.Max300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsCYAController._
import models.{CheckMode, Mode, Money}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.membersurrenderedbenefits._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class SurrenderedBenefitsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, memberIndex: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          memberDetails <- request.userAnswers
            .get(MemberDetailsPage(srn, memberIndex))
            .getOrRecoverJourney
          surrenderedBenefitsAmount <- request.userAnswers
            .get(SurrenderedBenefitsAmountPage(srn, memberIndex))
            .getOrRecoverJourney
          whenSurrenderedBenefits <- request.userAnswers
            .get(WhenDidMemberSurrenderBenefitsPage(srn, memberIndex))
            .getOrRecoverJourney
          whySurrenderedBenefits <- request.userAnswers
            .get(WhyDidMemberSurrenderBenefitsPage(srn, memberIndex))
            .getOrRecoverJourney
        } yield Ok(
          view(
            viewModel(
              srn,
              memberIndex,
              memberDetails.fullName,
              surrenderedBenefitsAmount,
              whenSurrenderedBenefits,
              whySurrenderedBenefits,
              mode
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, memberIndex: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(
          request.userAnswers
            .set(SurrenderedBenefitsJourneyStatus(srn), SectionStatus.InProgress)
            .remove(SurrenderedBenefitsMemberListPage(srn))
        )
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedUserAnswers)
      } yield submissionResult.getOrRecoverJourney(
        _ =>
          Redirect(
            navigator.nextPage(SurrenderedBenefitsCYAPage(srn, memberIndex), mode, request.userAnswers)
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
    mode: Mode
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = mode.fold(
        normal = "surrenderedBenefits.cya.title",
        check = "surrenderedBenefits.change.title"
      ),
      heading = mode.fold(
        normal = "surrenderedBenefits.cya.heading",
        check = Message("surrenderedBenefits.change.heading", memberName)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections = rows(
          srn,
          memberIndex,
          memberName,
          surrenderedBenefitsAmount,
          whenSurrenderedBenefits,
          whySurrenderedBenefits
        )
      ),
      refresh = None,
      buttonText = mode.fold(
        normal = "site.saveAndContinue",
        check = "site.continue"
      ),
      onSubmit = routes.SurrenderedBenefitsCYAController.onSubmit(srn, memberIndex, mode)
    )

  private def rows(
    srn: Srn,
    memberIndex: Max300,
    memberName: String,
    surrenderedBenefitsAmount: Money,
    whenSurrenderedBenefits: LocalDate,
    whySurrenderedBenefits: String
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
                .onSubmit(srn, memberIndex, CheckMode)
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
                .onSubmit(srn, memberIndex, CheckMode)
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
                .onSubmit(srn, memberIndex, CheckMode)
                .url
            ).withVisuallyHiddenContent(Message("surrenderedBenefits.cya.section.reason.hidden", memberName))
          )
        )
      )
    )
}
