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
import models.SchemeId.Srn
import utils.IntUtils.toInt
import uk.gov.hmrc.http.HeaderCarrier
import models._
import viewmodels.DisplayMessage
import models.requests.DataRequest
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import config.RefinedTypes._
import controllers.PsrControllerHelpers
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

type PclsData = (
  srn: Srn,
  memberName: String,
  index: Max300,
  amounts: PensionCommencementLumpSum,
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

object PclsCheckAnswersUtils extends CheckAnswersUtils[Max300, PclsData] with PsrControllerHelpers {

  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(PensionCommencementLumpSumPage(srn)).contains(true)

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.pcls.heading"))

  override def subheading(data: PclsData): Option[DisplayMessage] = Some(
    Message("nonsipp.summary.pcls.subheading", data.memberName)
  )

  override def summaryDataAsync(srn: Srn, index: Max300, mode: Mode)(using
    DataRequest[AnyContent],
    HeaderCarrier,
    ExecutionContext
  ): Future[Either[Result, PclsData]] =
    Future.successful(summaryData(srn, index, mode))

  def summaryData(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[Result, PclsData] = for {
    memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
    amounts <- request.userAnswers
      .get(PensionCommencementLumpSumAmountPage(srn, index))
      .getOrRecoverJourney
  } yield (
    srn,
    memberDetails.fullName,
    index,
    amounts,
    mode,
    false, // flag is not displayed on this tier
    request.year,
    request.currentVersion,
    request.previousVersion
  )

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(PensionCommencementLumpSumAmountPage.all())
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])
    .sortBy(i => request.userAnswers.get(MemberDetailsPage(srn, i)).map { case NameDOB(_, lastName, _) => lastName })

  override def viewModel(data: PclsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.memberName,
    data.index,
    data.amounts,
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
    amounts: PensionCommencementLumpSum,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "pclsCYA.normal.title",
        check = Message("pclsCYA.change.title.check", memberName),
        viewOnly = "pclsCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "pclsCYA.normal.heading",
        check = Message("pclsCYA.heading.check", memberName),
        viewOnly = "pclsCYA.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections = rows(
          srn,
          memberName,
          index,
          amounts,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.continue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "pclsCYA.viewOnly.title",
            heading = Message("pclsCYA.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def rows(
    srn: Srn,
    memberName: String,
    index: Max300,
    amounts: PensionCommencementLumpSum,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.membersName"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.amount.received", memberName),
            Message("£" + amounts.lumpSumAmount.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                .onPageLoad(srn, index, CheckMode)
                .url + "#received"
            ).withVisuallyHiddenContent(Message("pclsCYA.rows.amount.received.hidden", memberName))
          ),
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.amount.relevant", memberName),
            Message("£" + amounts.designatedPensionAmount.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                .onPageLoad(srn, index, mode)
                .url + "#relevant"
            ).withVisuallyHiddenContent(Message("pclsCYA.rows.amount.relevant.hidden", memberName))
          )
        )
      )
    )
}
