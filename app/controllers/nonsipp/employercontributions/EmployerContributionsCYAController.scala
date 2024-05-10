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

package controllers.nonsipp.employercontributions

import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import config.Refined._
import controllers.PSRController
import config.Constants
import cats.implicits.{catsSyntaxApplicativeId, toTraverseOps}
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import pages.nonsipp.employercontributions._
import services.{PsrSubmissionService, SaveService}
import cats.data.EitherT
import controllers.nonsipp.employercontributions.EmployerContributionsCYAController._
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{Heading2, Message}
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.{Inject, Named}

class EmployerContributionsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max300, page: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          membersName <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
          indexes <- buildSecondaryIndexes(srn, index)
          employerCYAs <- indexes.map(secondaryIndex => buildCYA(srn, index, secondaryIndex)).sequence
          orderedCYAs = employerCYAs.sortBy(_.secondaryIndex.value)
          _ <- recoverJourneyWhen(orderedCYAs.isEmpty)
        } yield Ok(
          view(viewModel(srn, membersName.fullName, index, page, orderedCYAs, mode))
        )
      ).merge
    }

  def onSubmit(srn: Srn, index: Max300, page: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val userAnswersWithSectionCompleted = buildSecondaryIndexes(srn, index).map(
        _.foldLeft(Try(request.userAnswers))(
          (userAnswers, secondaryIndex) =>
            userAnswers.set(EmployerContributionsCompleted(srn, index, secondaryIndex), SectionCompleted)
        )
      )

      (
        for {
          userAnswers <- EitherT(userAnswersWithSectionCompleted.pure[Future])
          updatedAnswers <- Future.fromTry(userAnswers).liftF
          _ <- saveService.save(updatedAnswers).liftF
          submissionResult <- psrSubmissionService
            .submitPsrDetailsWithUA(
              srn,
              updatedAnswers,
              fallbackCall = controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                .onPageLoad(srn, index, page, mode)
            )
            .liftF
        } yield submissionResult.getOrRecoverJourney(
          _ => Redirect(navigator.nextPage(EmployerContributionsCYAPage(srn), mode, updatedAnswers))
        )
      ).merge
    }

  private def buildSecondaryIndexes(srn: Srn, index: Max300)(
    implicit request: DataRequest[_]
  ): Either[Result, List[Max50]] =
    request.userAnswers
      .map(EmployerContributionsProgress.all(srn, index))
      .filter { case (_, status) => status.completed }
      .keys
      .toList
      .map(refineStringIndex[Max50.Refined])
      .sequence
      .getOrRecoverJourney

  private def buildCYA(srn: Srn, index: Max300, secondaryIndex: Max50)(
    implicit request: DataRequest[_]
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

object EmployerContributionsCYAController {

  def viewModel(
    srn: Srn,
    membersName: String,
    memberIndex: Max300,
    page: Int,
    employerCYAs: List[EmployerCYA],
    mode: Mode
  ): FormPageViewModel[CheckYourAnswersViewModel] = {

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.employerContributionsCYASize,
      employerCYAs.size,
      controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
        .onPageLoad(srn, memberIndex, _, NormalMode)
    )

    FormPageViewModel[CheckYourAnswersViewModel](
      title = mode.fold("checkYourAnswers.title", "employerContributionsCYA.title.change"),
      heading = mode.fold("checkYourAnswers.heading", Message("employerContributionsCYA.heading.change", membersName)),
      description = None,
      page = CheckYourAnswersViewModel(
        rows(srn, memberIndex, membersName, employerCYAs),
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
      buttonText = "site.saveAndContinue",
      onSubmit = routes.EmployerContributionsCYAController.onSubmit(srn, memberIndex, 1, mode)
    )
  }

  private def rows(
    srn: Srn,
    memberIndex: Max300,
    membersName: String,
    employerCYAs: List[EmployerCYA]
  ): List[CheckYourAnswersSection] =
    employerCYAs.zipWithIndex.map {
      case (employerCYA, journeyIndex) =>
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
                    .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
                    .url
                )
              case IdentityType.UKPartnership =>
                (
                  Message("employerContributionsCYA.row.employerId.partnership.reason", employerName),
                  Message("employerContributionsCYA.row.employerId.partnership.reason.hidden", employerName),
                  reason,
                  controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
                    .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
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
                    .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
                    .url
                )
              case IdentityType.UKPartnership =>
                (
                  Message("employerContributionsCYA.row.employerId.partnership", employerName),
                  Message("employerContributionsCYA.row.employerId.partnership.hidden", employerName),
                  id,
                  controllers.nonsipp.employercontributions.routes.PartnershipEmployerUtrController
                    .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
                    .url
                )
              case IdentityType.Other =>
                (
                  Message("employerContributionsCYA.row.employerId.other", employerName),
                  Message("employerContributionsCYA.row.employerId.other.hidden", employerName),
                  id,
                  controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
                    .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
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
                    .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
                    .url
                ).withVisuallyHiddenContent("employerContributionsCYA.row.employerName.hidden")
              ),
            CheckYourAnswersRowViewModel(
              Message("employerContributionsCYA.row.businessType", employerName),
              businessTypeMessageKey(identityType)
            ).withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.employercontributions.routes.EmployerTypeOfBusinessController
                  .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
                  .url
              ).withVisuallyHiddenContent(Message("employerContributionsCYA.row.businessType.hidden", employerName))
            ),
            CheckYourAnswersRowViewModel(employerIdOrReasonRowKey, employerIdOrReasonRowValue).withAction(
              SummaryAction("site.change", employerIdRedirectUrl)
                .withVisuallyHiddenContent(employerIdOrReasonRowHiddenKey)
            ),
            CheckYourAnswersRowViewModel(
              Message("employerContributionsCYA.row.contribution", employerName, membersName),
              s"£${totalEmployerContribution.displayAs}"
            ).withAction(
              SummaryAction(
                "site.change",
                controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
                  .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
                  .url
              ).withVisuallyHiddenContent(
                Message("employerContributionsCYA.row.contribution.hidden", employerName, membersName)
              )
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
                  .onPageLoad(srn, memberIndex, secondaryIndex, CheckMode)
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

  // just for readability in EmployerCYA (makes it clear the left on employerIdOrReason is the reason)
  private type Reason = String

  case class EmployerCYA(
    secondaryIndex: Max50,
    employerName: String,
    identityType: IdentityType,
    employerIdOrReason: Either[Reason, String],
    totalEmployerContribution: Money
  )
}
