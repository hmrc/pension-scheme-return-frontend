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

import pages.nonsipp.memberdetails.MembersDetailsPages
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.{Max300, Max50}
import controllers.PSRController
import config.Constants
import cats.implicits.catsSyntaxApplicativeId
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import pages.nonsipp.employercontributions.EmployerContributionsProgress.EmployerContributionsUserAnswersOps
import pages.nonsipp.employercontributions.{EmployerContributionsMemberListPage, EmployerContributionsSectionStatus}
import services.{PsrSubmissionService, SaveService}
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import controllers.nonsipp.employercontributions.EmployerContributionsMemberListController._
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.SectionJourneyStatus.InProgress
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class EmployerContributionsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  view: TwoColumnsTripleAction,
  psrSubmissionService: PsrSubmissionService,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form: Form[Boolean] = EmployerContributionsMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberMap = request.userAnswers.map(MembersDetailsPages(srn))
      val maxIndex: Either[Result, Int] = memberMap.keys
        .map(_.toInt)
        .maxOption
        .map(Right(_))
        .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val optionList: List[Option[NameDOB]] = maxIndex match {
        case Right(index) =>
          (0 to index).toList.map { index =>
            val memberOption = memberMap.get(index.toString)
            memberOption match {
              case Some(member) => Some(member)
              case None => None
            }
          }
        case Left(_) => List.empty
      }

      if (memberMap.nonEmpty) {
        optionList
          .zipWithRefinedIndex[Max300.Refined]
          .map { indexes =>
            val employerContributions = buildEmployerContributions(srn, indexes)
            val filledForm = request.userAnswers.fillForm(EmployerContributionsMemberListPage(srn), form)
            Ok(view(filledForm, viewModel(srn, page, mode, employerContributions)))
          }
          .merge
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val memberMap = request.userAnswers.map(MembersDetailsPages(srn))
      val maxIndex: Either[Result, Int] = memberMap.keys
        .map(_.toInt)
        .maxOption
        .map(Right(_))
        .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val optionList: List[Option[NameDOB]] = maxIndex match {
        case Right(index) =>
          (0 to index).toList.map { index =>
            val memberOption = memberMap.get(index.toString)
            memberOption match {
              case Some(member) => Some(member)
              case None => None
            }
          }
        case Left(_) => List.empty
      }

      if (optionList.flatten.size > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors => {
              optionList
                .zipWithRefinedIndex[Max300.Refined]
                .map { indexes =>
                  val employerContributions = buildEmployerContributions(srn, indexes)
                  BadRequest(
                    view(
                      errors,
                      EmployerContributionsMemberListController
                        .viewModel(srn, page, mode, employerContributions)
                    )
                  )
                }
                .merge
                .pure[Future]
            },
            value =>
              for {
                updatedUserAnswers <- Future.fromTry(
                  request.userAnswers
                    .set(
                      EmployerContributionsSectionStatus(srn),
                      if (value) {
                        SectionStatus.Completed
                      } else {
                        SectionStatus.InProgress
                      }
                    )
                    .set(EmployerContributionsMemberListPage(srn), value)
                )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (value) {
                  psrSubmissionService.submitPsrDetailsWithUA(srn, updatedUserAnswers)
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
                  )
              )
          )
      }
  }

  private def buildEmployerContributions(srn: Srn, indexes: List[(Max300, Option[NameDOB])])(
    implicit request: DataRequest[_]
  ): List[EmployerContributions] = indexes.flatMap {
    case (index, Some(nameDOB)) =>
      Some(
        EmployerContributions(
          memberIndex = index,
          employerFullName = nameDOB.fullName,
          contributions = request.userAnswers
            .employerContributionsProgress(srn, index)
            .map {
              case (secondaryIndex, status) =>
                Contributions(secondaryIndex, status)
            }
        )
      )
    case _ => None
  }
}

object EmployerContributionsMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "employerContributions.MemberList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    employerContributions: List[EmployerContributions]
  ): List[List[TableElem]] =
    employerContributions.map { employerContribution =>
      val noContributions = employerContribution.contributions.isEmpty
      val onlyInProgressContributions = employerContribution.contributions.forall(_.status.inProgress)

      if (noContributions || onlyInProgressContributions) {
        List(
          TableElem(
            employerContribution.employerFullName
          ),
          TableElem(
            Message("employerContributions.MemberList.status.no.contributions")
          ),
          TableElem.add(
            employerContribution.contributions.find(_.status.inProgress) match {
              case Some(Contributions(_, InProgress(url))) => url
              case None =>
                controllers.nonsipp.employercontributions.routes.EmployerNameController
                  .onSubmit(srn, employerContribution.memberIndex, refineMV(1), mode)
                  .url
            }
          ),
          TableElem.empty
        )
      } else {
        List(
          TableElem(
            employerContribution.employerFullName
          ),
          TableElem(
            if (employerContribution.contributions.size == 1) {
              Message(
                "employerContributions.MemberList.status.single.contribution",
                employerContribution.contributions.size
              )
            } else {
              Message(
                "employerContributions.MemberList.status.some.contributions",
                employerContribution.contributions.count(_.status.completed)
              )
            }
          ),
          TableElem.change(
            controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
              .onSubmit(srn, employerContribution.memberIndex, page = 1, CheckMode)
          ),
          TableElem.remove(
            controllers.nonsipp.employercontributions.routes.WhichEmployerContributionRemoveController
              .onSubmit(srn, employerContribution.memberIndex)
          )
        )
      }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    employerContributions: List[EmployerContributions]
  ): FormPageViewModel[ActionTableViewModel] = {

    val (title, heading) = if (employerContributions.size == 1) {
      ("employerContributions.MemberList.title", "employerContributions.MemberList.heading")
    } else {
      ("employerContributions.MemberList.title.plural", "employerContributions.MemberList.heading.plural")
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.employerContributionsMemberListSize,
      employerContributions.size,
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, employerContributions.size),
      heading = Message(heading, employerContributions.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage(
          "employerContributions.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "employerContributions.MemberList.paragraph2"
          ),
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, employerContributions.sortBy(_.employerFullName)),
        radioText = Message("employerContributions.MemberList.radios"),
        showInsetWithRadios = true,
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
      details = None,
      onSubmit = controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onSubmit(srn, page, mode)
    )
  }

  protected[employercontributions] case class EmployerContributions(
    memberIndex: Max300,
    employerFullName: String,
    contributions: List[Contributions]
  )

  protected[employercontributions] case class Contributions(
    contributionIndex: Max50,
    status: SectionJourneyStatus
  )
}
