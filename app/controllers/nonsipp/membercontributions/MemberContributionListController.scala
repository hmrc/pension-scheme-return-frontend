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

package controllers.nonsipp.membercontributions

import cats.implicits.{toBifunctorOps, toTraverseOps}
import com.google.inject.Inject
import config.Constants
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{CheckMode, Mode, Money, NameDOB, NormalMode, Pagination, UserAnswers}
import navigation.Navigator
import pages.nonsipp.employercontributions.EmployerContributionsMemberListPage
import pages.nonsipp.membercontributions.{MemberContributionsListPage, TotalMemberContributionPage}
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models._
import views.html.TwoColumnsTripleAction

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class MemberContributionListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = MemberContributionListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val userAnswers = request.userAnswers
      val memberList = userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = MemberContributionListController
          .viewModel(srn, page, mode, memberList, userAnswers)
        val filledForm =
          request.userAnswers.get(MemberContributionsListPage(srn)).fold(form)(form.fill)
        Ok(view(filledForm, viewModel))
      } else {
        Redirect(
          controllers.routes.JourneyRecoveryController.onPageLoad()
        )
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val userAnswers = request.userAnswers
      val memberList = userAnswers.membersDetails(srn)
      val memberListSize = memberList.size

      if (memberListSize > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(MemberContributionsListPage(srn), mode, request.userAnswers)
          )
        )
      } else {

        form
          .bindFromRequest()
          .fold(
            errors =>
              Future.successful(
                BadRequest(
                  view(
                    errors,
                    MemberContributionListController.viewModel(srn, page, mode, memberList, userAnswers)
                  )
                )
              ),
            value =>
              for {
                updatedUserAnswers <- buildUserAnswerBySelection(srn, value, memberListSize)
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (value) {
                  psrSubmissionService.submitPsrDetails(srn)(
                    implicitly,
                    implicitly,
                    request = DataRequest(request.request, updatedUserAnswers)
                  )
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

  private def buildUserAnswerBySelection(srn: Srn, selection: Boolean, memberListSize: Int)(
    implicit request: DataRequest[_]
  ): Future[UserAnswers] = {
    val userAnswerWithMemberContList = request.userAnswers.set(MemberContributionsListPage(srn), selection)

    if (selection) {
      val indexes = (1 to memberListSize)
        .map(i => refineV[OneTo300](i).leftMap(new Exception(_)).toTry)
        .toList
        .sequence

      Future.fromTry(
        indexes.fold(
          _ => userAnswerWithMemberContList,
          index =>
            index.foldLeft(userAnswerWithMemberContList) {
              case (uaTry, index) =>
                val optTotalMemberContribution = request.userAnswers.get(TotalMemberContributionPage(srn, index))
                for {
                  ua <- uaTry
                  ua1 <- ua.set(TotalMemberContributionPage(srn, index), optTotalMemberContribution.getOrElse(Money(0)))
                } yield ua1
            }
        )
      )
    } else {
      Future.fromTry(userAnswerWithMemberContList)
    }
  }
}

object MemberContributionListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "ReportContribution.MemberList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[NameDOB],
    userAnswers: UserAnswers
  ): List[List[TableElem]] =
    memberList.zipWithIndex.map {
      case (memberName, index) =>
        refineV[OneTo300](index + 1) match {
          case Left(_) => Nil
          case Right(nextIndex) =>
            val contributions = userAnswers.get(TotalMemberContributionPage(srn, nextIndex))
            if (contributions.nonEmpty) {
              List(
                TableElem(
                  memberName.fullName
                )
              ) ++ buildMutableTable(srn, nextIndex)
            } else {
              List(
                TableElem(
                  memberName.fullName
                )
              ) ++ addOnlyTable(srn, nextIndex, mode)
            }
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[NameDOB],
    userAnswers: UserAnswers
  ): FormPageViewModel[ActionTableViewModel] = {

    val (title, heading) =
      if (memberList.size == 1) {
        ("ReportContribution.MemberList.title", "ReportContribution.MemberList.heading")
      } else {
        ("ReportContribution.MemberList.title.plural", "ReportContribution.MemberList.heading.plural")
      }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertiesSize,
      memberList.size,
      controllers.nonsipp.membercontributions.routes.MemberContributionListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage(
          "ReportContribution.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "ReportContribution.MemberList.paragraph2"
          ),
        showInsetWithRadios = true,
        head = Some(List(TableElem("Member name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("ReportContribution.MemberList.radios"),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "ReportContribution.MemberList.pagination.label",
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
      onSubmit =
        controllers.nonsipp.membercontributions.routes.MemberContributionListController.onSubmit(srn, page, mode)
    )
  }

  private def buildMutableTable(
    srn: Srn,
    nextIndex: Refined[Int, OneTo300]
  ): List[TableElem] =
    List(
      TableElem(
        "Member contributions reported"
      ),
      TableElem(
        LinkMessage(
          "Change",
          controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
            .onPageLoad(srn, nextIndex, CheckMode)
            .url
        )
      ),
      TableElem(
        LinkMessage(
          "Remove",
          controllers.nonsipp.membercontributions.routes.RemoveMemberContributionController
            .onPageLoad(srn, nextIndex)
            .url
        )
      )
    )

  private def addOnlyTable(
    srn: Srn,
    nextIndex: Refined[Int, OneTo300],
    mode: Mode
  ): List[TableElem] =
    List(
      TableElem(
        "No member contributions"
      ),
      TableElem(
        LinkMessage(
          "Add",
          controllers.nonsipp.membercontributions.routes.TotalMemberContributionController
            .onSubmit(srn, nextIndex, mode)
            .url
        )
      ),
      TableElem("")
    )
}