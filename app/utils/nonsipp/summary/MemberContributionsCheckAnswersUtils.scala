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
import play.api.mvc._
import utils.IntUtils.toInt
import cats.implicits.toTraverseOps
import viewmodels.models.SummaryPageEntry._
import models._
import models.requests.DataRequest
import viewmodels.implicits._
import pages.nonsipp.membercontributions.{
  AllTotalMemberContributionPages,
  MemberContributionsPage,
  TotalMemberContributionPage
}
import config.RefinedTypes.Max300
import controllers.PsrControllerHelpers
import cats.data.EitherT
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.Future

type MemberContributionsData = (
  srn: Srn,
  memberName: String,
  index: Max300,
  contributions: Money,
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

object MemberContributionsCheckAnswersUtils extends PsrControllerHelpers {

  def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(AllTotalMemberContributionPages(srn))
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])
    .sortBy(i => request.userAnswers.get(MemberDetailsPage(srn, i)).map { case NameDOB(_, lastName, _) => lastName })

  def sectionEntries(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): EitherT[Future, Result, List[SummaryPageEntry]] =
    EitherT(Future.successful(for {
      datas <- indexes(srn).map(summaryData(srn, _, mode)).sequence
      isReported = request.userAnswers.get(MemberContributionsPage(srn)).contains(true)
      heading = Heading(Message("nonsipp.summary.memberContributions.heading"))
      sections = datas.flatMap { data =>
        val vm = viewModel(data)
        val subheading = Subheading(Message("nonsipp.summary.memberContributions.subheading", data.memberName))

        List(subheading, Section(vm.page.toSummaryViewModel()))
      }
      body = if (isReported) sections else List(MessageLine(Message("nonsipp.summary.message.noneRecorded")))
    } yield List(heading) ++ body))

  def summaryData(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[Result, MemberContributionsData] = for {
    memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
    contribution <- request.userAnswers.get(TotalMemberContributionPage(srn, index)).getOrRecoverJourney
  } yield (
    srn,
    memberDetails.fullName,
    index,
    contribution,
    mode,
    false, // flag is not displayed on this tier
    request.year,
    request.currentVersion,
    request.previousVersion
  )

  def viewModel(data: MemberContributionsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.memberName,
    data.index,
    data.contributions,
    data.mode,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion
  )

  def viewModel(
    srn: Srn,
    memberName: String,
    index: Max300,
    contributions: Money,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode
        .fold(
          normal = "memberContributionCYA.title",
          check = "memberContributionCYA.change.title",
          viewOnly = "memberContributionCYA.viewOnly.title"
        ),
      heading = mode.fold(
        normal = "memberContributionCYA.heading",
        check = Message(
          "memberContributionCYA.change.heading",
          memberName
        ),
        viewOnly = "memberContributionCYA.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          memberName,
          index,
          contributions,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
        .onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "memberContributionCYA.viewOnly.title",
            heading = Message("memberContributionCYA.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def sections(
    srn: Srn,
    memberName: String,
    index: Max300,
    contributions: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("memberContributionCYA.section.memberName.header"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("memberContributionCYA.section.memberName", memberName),
            Message("memberContributionCYA.section.amount", contributions.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membercontributions.routes.TotalMemberContributionController
                .onSubmit(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(Message("memberContributionCYA.section.hide", memberName))
          )
        )
      )
    )

}
