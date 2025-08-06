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
import pages.nonsipp.memberpensionpayments.{PensionPaymentsReceivedPage, TotalAmountPensionPaymentsPage}
import uk.gov.hmrc.http.HeaderCarrier
import models._
import viewmodels.DisplayMessage
import models.requests.DataRequest
import config.RefinedTypes._
import controllers.PsrControllerHelpers
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

type MemberPaymentsData = (
  srn: Srn,
  memberName: String,
  index: Max300,
  pensionPayments: Money,
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

object MemberPaymentsCheckAnswersUtils extends CheckAnswersUtils[Max300, MemberPaymentsData] with PsrControllerHelpers {

  override def isReported(srn: Srn)(using request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(PensionPaymentsReceivedPage(srn)).contains(true)

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.memberPayments.heading"))

  override def subheading(data: MemberPaymentsData): Option[DisplayMessage] = Some(
    Message("nonsipp.summary.memberPayments.subheading", data.memberName)
  )

  override def summaryDataAsync(srn: Srn, index: Max300, mode: Mode)(using
    DataRequest[AnyContent],
    HeaderCarrier,
    ExecutionContext
  ): Future[Either[Result, MemberPaymentsData]] = Future.successful(summaryData(srn, index, mode))

  def summaryData(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[Result, MemberPaymentsData] = for {
    memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
    pensionPayment <- request.userAnswers.get(TotalAmountPensionPaymentsPage(srn, index)).getOrRecoverJourney
  } yield (
    srn,
    memberDetails.fullName,
    index,
    pensionPayment,
    mode,
    false,
    request.year,
    request.currentVersion,
    request.previousVersion
  )

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(TotalAmountPensionPaymentsPage.all())
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])
    .sortBy(i => request.userAnswers.get(MemberDetailsPage(srn, i)).map { case NameDOB(_, lastName, _) => lastName })

  override def viewModel(data: MemberPaymentsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.memberName,
    data.index,
    data.pensionPayments,
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
    pensionPayments: Money,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "memberPensionPaymentsCYA.title",
        check = "memberPensionPaymentsCYA.change.title",
        viewOnly = "memberPensionPaymentsCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "memberPensionPaymentsCYA.heading",
        check = Message(
          "memberPensionPaymentsCYA.change.heading",
          memberName
        ),
        viewOnly = Message("memberPensionPaymentsCYA.viewOnly.heading", memberName)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          memberName,
          index,
          pensionPayments,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
        .onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "memberPensionPaymentsCYA.viewOnly.title",
            heading = Message("memberPensionPaymentsCYA.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
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
    pensionPayments: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("memberPensionPaymentsCYA.section.memberName.header"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("memberPensionPaymentsCYA.section.memberName", memberName),
            Message("memberPensionPaymentsCYA.section.amount", pensionPayments.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
                .onSubmit(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(Message("memberPensionPaymentsCYA.section.hide", memberName))
          )
        )
      )
    )
}
