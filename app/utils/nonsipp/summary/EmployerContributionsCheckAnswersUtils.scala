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

import pages.nonsipp.employercontributions._
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{AnyContent, Result}
import utils.ListUtils.ListOps
import config.Constants
import models.SchemeId.Srn
import utils.IntUtils.toInt
import cats.implicits.toTraverseOps
import uk.gov.hmrc.http.HeaderCarrier
import models.{Mode, _}
import viewmodels.DisplayMessage
import models.requests.DataRequest
import config.RefinedTypes.{Max300, Max50}
import controllers.PsrControllerHelpers
import viewmodels.DisplayMessage.{Heading2, Message}
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel, _}

import scala.concurrent.{ExecutionContext, Future}

type EmployerContributionsData = (
  srn: Srn,
  membersName: String,
  memberIndex: Max300,
  page: Int,
  employerCYAs: List[EmployerCYA],
  mode: Mode,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int]
)

case class EmployerCYA(
  secondaryIndex: Max50,
  employerName: String,
  identityType: IdentityType,
  employerIdOrReason: Either[String, String],
  totalEmployerContribution: Money
)

object EmployerContributionsCheckAnswersUtils
    extends CheckAnswersUtils[Max300, EmployerContributionsData]
    with PsrControllerHelpers {

  override def heading: Option[DisplayMessage] = Some(Message("nonsipp.summary.employerContributions.heading"))

  override def subheading(data: EmployerContributionsData): Option[DisplayMessage] =
    Some(Message("nonsipp.summary.employerContributions.subheading", data.membersName))

  override def summaryDataAsync(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Result, EmployerContributionsData]] = Future.successful(summaryData(srn, index, 1, mode))

  override def indexes(srn: Srn)(using request: DataRequest[AnyContent]): List[Max300] = request.userAnswers
    .map(EmployerContributionsProgress.all())
    .filter { case (_, status) => status.exists(_._2.completed) }
    .keys
    .toList
    .flatMap(refineStringIndex[Max300.Refined])

  override def viewModel(data: EmployerContributionsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.membersName,
    data.memberIndex,
    data.page,
    data.employerCYAs,
    data.mode,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion
  )

  def summaryData(srn: Srn, memberIndex: Max300, page: Int, mode: Mode)(using
    request: DataRequest[AnyContent]
  ): Either[Result, EmployerContributionsData] =
    for {
      membersName <- request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney
      indexes <- buildCompletedSecondaryIndexes(memberIndex)
      employerCYAs <- indexes.map(secondaryIndex => buildCYA(srn, memberIndex, secondaryIndex)).sequence
      orderedCYAs = employerCYAs.sortBy(_.secondaryIndex.value)
      _ <- recoverJourneyWhen(
        orderedCYAs.isEmpty,
        if (mode.isViewOnlyMode) {
          controllers.routes.JourneyRecoveryController.onPageLoad()
        } else {
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoad(srn, 1, mode)
        }
      )
    } yield (
      srn,
      membersName.fullName,
      memberIndex,
      page,
      orderedCYAs,
      mode,
      false,
      request.year,
      request.currentVersion,
      request.previousVersion
    )

  def buildCompletedSecondaryIndexes(index: Max300)(implicit
    request: DataRequest[?]
  ): Either[Result, List[Max50]] =
    request.userAnswers
      .map(EmployerContributionsProgress.all(index))
      .filter { case (_, status) => status.completed }
      .keys
      .toList
      .map(refineStringIndex[Max50.Refined])
      .sequence
      .getOrRecoverJourney

  def buildCYA(srn: Srn, index: Max300, secondaryIndex: Max50)(implicit
    request: DataRequest[?]
  ): Either[Result, EmployerCYA] =
    for {
      employerName <- request.userAnswers
        .get(EmployerNamePage(srn, index, secondaryIndex))
        .getOrRecoverJourney
      employerType <- request.userAnswers
        .get(EmployerTypeOfBusinessPage(srn, index, secondaryIndex))
        .getOrRecoverJourney
      totalEmployerContribution <- request.userAnswers
        .get(TotalEmployerContributionPage(srn, index, secondaryIndex))
        .getOrRecoverJourney
      employerIdOrReason <- (employerType: @unchecked) match {
        case IdentityType.UKCompany =>
          request.userAnswers
            .get(EmployerCompanyCrnPage(srn, index, secondaryIndex))
            .getOrRecoverJourney
            .map(_.value.map(_.value))
        case IdentityType.UKPartnership =>
          request.userAnswers
            .get(PartnershipEmployerUtrPage(srn, index, secondaryIndex))
            .getOrRecoverJourney
            .map(_.value.map(_.value))
        case IdentityType.Other =>
          request.userAnswers
            .get(OtherEmployeeDescriptionPage(srn, index, secondaryIndex))
            .getOrRecoverJourney
            .map(Right(_))
      }
    } yield EmployerCYA(
      secondaryIndex,
      employerName,
      employerType,
      employerIdOrReason,
      totalEmployerContribution
    )

  def viewModel(
    srn: Srn,
    membersName: String,
    memberIndex: Max300,
    page: Int,
    employerCYAs: List[EmployerCYA],
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] = {

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.employerContributionsCYASize,
      employerCYAs.size,
      (optYear, optCurrentVersion, optPreviousVersion) match {
        case (Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
            .onPageLoadViewOnly(srn, memberIndex, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
            .onPageLoad(srn, memberIndex, _, mode)
      }
    )

    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        "employerContributionsCYA.title.normal",
        "employerContributionsCYA.title.change",
        "employerContributionsCYA.title.viewOnly"
      ),
      heading = mode.fold(
        "employerContributionsCYA.heading.normal",
        Message("employerContributionsCYA.heading.change", membersName),
        Message("employerContributionsCYA.heading.viewOnly", membersName)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        rows(srn, mode, memberIndex, membersName, employerCYAs),
        inset = if (employerCYAs.length == 50) Some("employerContributionsCYA.inset") else None,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "employerContributions.MemberList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
        .onSubmit(srn, memberIndex, 1, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "employerContributionsCYA.title.viewOnly",
            heading = Message("employerContributionsCYA.heading.viewOnly", membersName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                  .onSubmit(srn, memberIndex, page, mode)
            }
          )
        )
      } else {
        None
      }
    )
  }

  def rows(
    srn: Srn,
    mode: Mode,
    memberIndex: Max300,
    membersName: String,
    employerCYAs: List[EmployerCYA]
  ): List[CheckYourAnswersSection] = {
    employerCYAs.zipWithIndex.map { case (employerCYA, journeyIndex) =>
      import employerCYA._

      val (
        employerIdOrReasonRowKey,
        employerIdOrReasonRowHiddenKey,
        employerIdOrReasonRowValue,
        employerIdRedirectUrl
      ) = employerIdOrReason.fold(
        reason =>
          (identityType: @unchecked) match {
            case IdentityType.UKCompany =>
              (
                Message("employerContributionsCYA.row.employerId.company.reason", employerName),
                Message("employerContributionsCYA.row.employerId.company.reason.hidden", employerName),
                reason,
                controllers.nonsipp.employercontributions.routes.EmployerCompanyCrnController
                  .onPageLoad(
                    srn,
                    memberIndex,
                    secondaryIndex,
                    mode match {
                      case ViewOnlyMode => NormalMode
                      case _ => mode
                    }
                  )
                  .url
              )
            case IdentityType.UKPartnership =>
              (
                Message("employerContributionsCYA.row.employerId.partnership.reason", employerName),
                Message("employerContributionsCYA.row.employerId.partnership.reason.hidden", employerName),
                reason,
                controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
                  .onPageLoad(
                    srn,
                    memberIndex,
                    secondaryIndex,
                    mode match {
                      case ViewOnlyMode => NormalMode
                      case _ => mode
                    }
                  )
                  .url
              )
          },
        id =>
          (identityType: @unchecked) match {
            case IdentityType.UKCompany =>
              (
                Message("employerContributionsCYA.row.employerId.company", employerName),
                Message("employerContributionsCYA.row.employerId.company.hidden", employerName),
                id,
                controllers.nonsipp.employercontributions.routes.EmployerCompanyCrnController
                  .onPageLoad(
                    srn,
                    memberIndex,
                    secondaryIndex,
                    mode match {
                      case ViewOnlyMode => NormalMode
                      case _ => mode
                    }
                  )
                  .url
              )
            case IdentityType.UKPartnership =>
              (
                Message("employerContributionsCYA.row.employerId.partnership", employerName),
                Message("employerContributionsCYA.row.employerId.partnership.hidden", employerName),
                id,
                controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
                  .onPageLoad(
                    srn,
                    memberIndex,
                    secondaryIndex,
                    mode match {
                      case ViewOnlyMode => NormalMode
                      case _ => mode
                    }
                  )
                  .url
              )
            case IdentityType.Other =>
              (
                Message("employerContributionsCYA.row.employerId.other", employerName),
                Message("employerContributionsCYA.row.employerId.other.hidden", employerName),
                id,
                controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
                  .onPageLoad(
                    srn,
                    memberIndex,
                    secondaryIndex,
                    mode match {
                      case ViewOnlyMode => NormalMode
                      case _ => mode
                    }
                  )
                  .url
              )
          }
      )

      CheckYourAnswersSection(
        if (employerCYAs.length == 1) None
        else
          Some(
            Heading2.medium(Message("employerContributionsCYA.section.heading", employerCYA.secondaryIndex.value))
          ),
        List(
          CheckYourAnswersRowViewModel("employerContributionsCYA.row.memberName", membersName),
          CheckYourAnswersRowViewModel("employerContributionsCYA.row.employerName", employerName)
            .withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.employercontributions.routes.EmployerNameController
                  .onPageLoad(
                    srn,
                    memberIndex,
                    secondaryIndex,
                    mode match {
                      case ViewOnlyMode => NormalMode
                      case _ => mode
                    }
                  )
                  .url
              ).withVisuallyHiddenContent("employerContributionsCYA.row.employerName.hidden")
            ),
          CheckYourAnswersRowViewModel(
            Message("employerContributionsCYA.row.businessType", employerName),
            businessTypeMessageKey(identityType)
          ).withChangeAction(
            controllers.nonsipp.employercontributions.routes.EmployerTypeOfBusinessController
              .onPageLoad(
                srn,
                memberIndex,
                secondaryIndex,
                mode match {
                  case ViewOnlyMode => NormalMode
                  case _ => CheckMode
                }
              )
              .url,
            hidden = Message("employerContributionsCYA.row.businessType.hidden", employerName)
          ),
          CheckYourAnswersRowViewModel(employerIdOrReasonRowKey, employerIdOrReasonRowValue).withAction(
            SummaryAction("site.change", employerIdRedirectUrl)
              .withVisuallyHiddenContent(employerIdOrReasonRowHiddenKey)
          ),
          CheckYourAnswersRowViewModel(
            Message("employerContributionsCYA.row.contribution", employerName, membersName),
            s"£${totalEmployerContribution.displayAs}"
          ).withChangeAction(
            controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
              .onPageLoad(
                srn,
                memberIndex,
                secondaryIndex,
                mode match {
                  case ViewOnlyMode => NormalMode
                  case _ => CheckMode
                }
              )
              .url,
            hidden = Message("employerContributionsCYA.row.contribution.hidden", employerName, membersName)
          )
          // append "any contributions by other employers" row to final section
        ) :?+ Option.when(journeyIndex + 1 == employerCYAs.length)(
          CheckYourAnswersRowViewModel(
            Message("employerContributionsCYA.row.contributionOtherEmployer", membersName),
            "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.employercontributions.routes.ContributionsFromAnotherEmployerController
                .onPageLoad(
                  srn,
                  memberIndex,
                  secondaryIndex,
                  mode match {
                    case ViewOnlyMode => NormalMode
                    case _ => mode
                  }
                )
                .url
            ).withVisuallyHiddenContent(
              Message("employerContributionsCYA.row.contributionOtherEmployer", membersName)
            )
          )
        )
      )
    }
  }

  def businessTypeMessageKey(identityType: IdentityType): String = identityType match {
    case IdentityType.Individual => "identityType.individual"
    case IdentityType.UKCompany => "identityType.company"
    case IdentityType.UKPartnership => "identityType.partnership"
    case IdentityType.Other => "identityType.other"
  }
}
