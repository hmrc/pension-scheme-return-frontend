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

import pages.nonsipp.employercontributions._
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{AnyContent, Result}
import utils.ListUtils.ListOps
import models.SchemeId.Srn
import utils.IntUtils.given
import cats.implicits.toTraverseOps
import models.{CheckMode, _}
import play.api.i18n._
import models.requests.DataRequest
import config.RefinedTypes.{Max300, Max50}
import controllers.PsrControllerHelpers
import viewmodels.DisplayMessage._
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersSection, SummaryAction}

object EmployerContributionsCheckAnswersSectionUtils extends PsrControllerHelpers {

  def employerContributionsSections(srn: Srn, index: Max300, mode: Mode)(using
    request: DataRequest[AnyContent],
    messages: Messages
  ): Either[Result, List[CheckYourAnswersSection]] =
    for {
      membersName <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
      indexes <- buildCompletedSecondaryIndexes(index)
      employerCYAs <- indexes.map(secondaryIndex => buildCYA(srn, index, secondaryIndex)).sequence
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
    } yield rows(
      srn,
      mode,
      index,
      membersName.fullName,
      employerCYAs
    )

  def rows(
    srn: Srn,
    mode: Mode,
    memberIndex: Max300,
    membersName: String,
    employerCYAs: List[EmployerCYA]
  ): List[CheckYourAnswersSection] =
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

  private def businessTypeMessageKey(identityType: IdentityType): String = identityType match {
    case IdentityType.Individual => "identityType.individual"
    case IdentityType.UKCompany => "identityType.company"
    case IdentityType.UKPartnership => "identityType.partnership"
    case IdentityType.Other => "identityType.other"
  }

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
}

case class EmployerCYA(
  secondaryIndex: Max50,
  employerName: String,
  identityType: IdentityType,
  employerIdOrReason: Either[String, String],
  totalEmployerContribution: Money
)
