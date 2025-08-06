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

package utils.nonsipp.summary

import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import models.SchemeId.Srn
import utils.IntUtils.toInt
import cats.implicits.toShow
import uk.gov.hmrc.http.HeaderCarrier
import pages.nonsipp.membersurrenderedbenefits._
import viewmodels.{DisplayMessage, Margin}
import models.requests.DataRequest
import config.RefinedTypes.Max300
import controllers.PsrControllerHelpers
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

type SurrenderedBenefitsData = (
  srn: Srn,
  memberIndex: Max300,
  memberName: String,
  surrenderedBenefitsAmount: Money,
  whenSurrenderedBenefits: LocalDate,
  whySurrenderedBenefits: String,
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

object MemberSurrenderedBenefitsCheckAnswersUtils
    extends CheckAnswersUtils[Max300, SurrenderedBenefitsData]
    with PsrControllerHelpers {

  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(SurrenderedBenefitsPage(srn)).contains(true)

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.memberSurrenderedBenefits.heading"))

  override def subheading(data: SurrenderedBenefitsData): Option[DisplayMessage] = Some(
    Message("nonsipp.summary.memberSurrenderedBenefits.subheading", data.memberName)
  )

  override def summaryDataAsync(srn: Srn, index: Max300, mode: Mode)(using
    DataRequest[AnyContent],
    HeaderCarrier,
    ExecutionContext
  ): Future[Either[Result, SurrenderedBenefitsData]] =
    Future.successful(summaryData(srn, index, mode))

  def summaryData(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[Result, SurrenderedBenefitsData] = for {
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
  } yield (
    srn,
    index,
    memberDetails.fullName,
    surrenderedBenefitsAmount,
    whenSurrenderedBenefits,
    whySurrenderedBenefits,
    mode,
    false,
    request.year,
    request.currentVersion,
    request.previousVersion
  )

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(SurrenderedBenefitsCompleted.all())
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])
    .sortBy(i => request.userAnswers.get(MemberDetailsPage(srn, i)).map { case NameDOB(_, lastName, _) => lastName })

  override def viewModel(data: SurrenderedBenefitsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.memberIndex,
    data.memberName,
    data.surrenderedBenefitsAmount,
    data.whenSurrenderedBenefits,
    data.whySurrenderedBenefits,
    data.mode,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion
  )

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
      onSubmit = controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
        .onSubmit(srn, memberIndex, mode),
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
                controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
                  .onSubmit(srn, memberIndex, mode)
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
