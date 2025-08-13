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

import pages.nonsipp.memberdetails._
import play.api.mvc._
import models.SchemeId.Srn
import utils.IntUtils.toInt
import cats.implicits.{toShow, toTraverseOps}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.DisplayMessage
import models.requests.DataRequest
import viewmodels.implicits._
import utils.MessageUtils.booleanToMessage
import config.RefinedTypes.Max300
import controllers.PsrControllerHelpers
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

type MemberDeteilsData = (
  index: Max300,
  srn: Srn,
  mode: Mode,
  memberDetails: NameDOB,
  hasNINO: Boolean,
  maybeNino: Option[Nino],
  maybeNoNinoReason: Option[String],
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

object MemberDetailsCheckAnswersUtils extends CheckAnswersUtils[Max300, MemberDeteilsData] with PsrControllerHelpers {

  override def heading: Option[DisplayMessage] = None
  override def heading(allData: List[MemberDeteilsData]): Option[DisplayMessage] = Some(
    if (allData.length == 1) Message("nonsipp.summary.memberDetails.single.heading")
    else Message("nonsipp.summary.memberDetails.heading", allData.length)
  )

  override def subheading(data: MemberDeteilsData): Option[DisplayMessage] = Some(
    Message("nonsipp.summary.memberDetails.subheading", data.memberDetails.fullName)
  )

  override def summaryDataAsync(srn: Srn, index: Max300, mode: Mode)(using
    DataRequest[AnyContent],
    HeaderCarrier,
    ExecutionContext
  ): Future[Either[Result, MemberDeteilsData]] =
    Future.successful(summaryData(srn, index, mode).getOrRecoverJourney)

  def summaryData(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Option[MemberDeteilsData] =
    for {
      memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index))
      hasNINO <- request.userAnswers.get(DoesMemberHaveNinoPage(srn, index))
      maybeNino <- Option.when(hasNINO)(request.userAnswers.get(MemberDetailsNinoPage(srn, index))).sequence
      maybeNoNinoReason <- Option.when(!hasNINO)(request.userAnswers.get(NoNINOPage(srn, index))).sequence
    } yield (
      index,
      srn,
      mode,
      memberDetails,
      hasNINO,
      maybeNino,
      maybeNoNinoReason,
      false,
      request.year,
      request.currentVersion,
      request.previousVersion
    )

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(MembersDetailsPages(srn))
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])
    .sortBy(i => request.userAnswers.get(MemberDetailsPage(srn, i)).map { case NameDOB(_, lastName, _) => lastName })

  override def viewModel(data: MemberDeteilsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.index,
    data.srn,
    data.mode,
    data.memberDetails,
    data.hasNINO,
    data.maybeNino,
    data.maybeNoNinoReason,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion
  )

  private def rows(
    index: Max300,
    srn: Srn,
    memberDetails: NameDOB,
    hasNINO: Boolean,
    maybeNino: Option[Nino],
    maybeNoNinoReason: Option[String]
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel("memberDetails.firstName", memberDetails.firstName)
        .withAction(
          SummaryAction(
            "site.change",
            controllers.nonsipp.memberdetails.routes.MemberDetailsController
              .onPageLoad(srn, index, CheckMode)
              .url + "#firstName"
          ).withVisuallyHiddenContent("memberDetails.firstName")
        ),
      CheckYourAnswersRowViewModel("memberDetails.lastName", memberDetails.lastName)
        .withAction(
          SummaryAction(
            "site.change",
            controllers.nonsipp.memberdetails.routes.MemberDetailsController
              .onPageLoad(srn, index, CheckMode)
              .url + "#lastName"
          ).withVisuallyHiddenContent("memberDetails.lastName")
        ),
      CheckYourAnswersRowViewModel("memberDetails.dateOfBirth", memberDetails.dob.show)
        .withAction(
          SummaryAction(
            "site.change",
            controllers.nonsipp.memberdetails.routes.MemberDetailsController
              .onPageLoad(srn, index, CheckMode)
              .url + "#dateOfBirth"
          ).withVisuallyHiddenContent("memberDetails.dateOfBirth")
        ),
      CheckYourAnswersRowViewModel(
        Message("nationalInsuranceNumber.heading", memberDetails.fullName),
        booleanToMessage(hasNINO)
      ).withAction(
        SummaryAction(
          "site.change",
          controllers.nonsipp.memberdetails.routes.DoesSchemeMemberHaveNINOController
            .onPageLoad(srn, index, CheckMode)
            .url
        )
          .withVisuallyHiddenContent(("memberDetailsCYA.nationalInsuranceNumber.hidden", memberDetails.fullName))
      )
    ) ++
      ninoRow(maybeNino, memberDetails.fullName, srn, index) ++
      noNinoReasonRow(maybeNoNinoReason, memberDetails.fullName, srn, index)

  private def ninoRow(
    maybeNino: Option[Nino],
    memberName: String,
    srn: Srn,
    index: Max300
  ): List[CheckYourAnswersRowViewModel] =
    maybeNino.fold(List.empty[CheckYourAnswersRowViewModel])(nino =>
      List(
        CheckYourAnswersRowViewModel(Message("memberDetailsNino.heading", memberName), nino.value)
          .withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberdetails.routes.MemberDetailsNinoController.onPageLoad(srn, index, CheckMode).url
            )
              .withVisuallyHiddenContent(("memberDetailsCYA.nino.hidden", memberName))
          )
      )
    )

  private def noNinoReasonRow(
    maybeNoNinoReason: Option[String],
    memberName: String,
    srn: Srn,
    index: Max300
  ): List[CheckYourAnswersRowViewModel] =
    maybeNoNinoReason.fold(List.empty[CheckYourAnswersRowViewModel])(noNinoReason =>
      List(
        CheckYourAnswersRowViewModel(Message("noNINO.heading", memberName), noNinoReason)
          .withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberdetails.routes.NoNINOController.onPageLoad(srn, index, CheckMode).url
            )
              .withVisuallyHiddenContent(("memberDetailsCYA.noNINO.hidden", memberName))
          )
      )
    )

  def viewModel(
    index: Max300,
    srn: Srn,
    mode: Mode,
    memberDetails: NameDOB,
    hasNINO: Boolean,
    maybeNino: Option[Nino],
    maybeNoNinoReason: Option[String],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel(
      mode = mode,
      title = mode.fold(
        normal = "checkYourAnswers.title",
        check = "changeMemberDetails.title",
        viewOnly = "memberDetailsCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "checkYourAnswers.heading",
        check = Message("changeMemberDetails.heading", memberDetails.fullName),
        viewOnly = Message("memberDetailsCYA.viewOnly.title", memberDetails.fullName)
      ),
      description = None,
      page = CheckYourAnswersViewModel.singleSection(
        rows(index, srn, memberDetails, hasNINO, maybeNino, maybeNoNinoReason)
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit =
        controllers.nonsipp.memberdetails.routes.SchemeMemberDetailsAnswersController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "memberDetailsCYA.viewOnly.title",
            heading = Message("memberDetailsCYA.viewOnly.heading", memberDetails.fullName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.memberdetails.routes.SchemeMemberDetailsAnswersController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.memberdetails.routes.SchemeMemberDetailsAnswersController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )
}
