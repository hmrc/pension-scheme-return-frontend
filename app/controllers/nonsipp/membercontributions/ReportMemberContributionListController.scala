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

import com.google.inject.Inject
import config.Constants
import config.Constants.maxNotRelevant
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{CheckOrChange, Mode, Money, NameDOB, NormalMode, Pagination, UserAnswers}
import navigation.Navigator
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberpayments.ReportMemberContributionListPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction

import javax.inject.Named

class ReportMemberContributionListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form: Form[Boolean] = ReportMemberContributionListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val userAnswers = request.userAnswers
      val memberList = userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = ReportMemberContributionListController
          .viewModel(srn, page, mode, memberList, userAnswers)
        Ok(view(form, viewModel))
      } else {
        Redirect(controllers.routes.UnauthorisedController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val userAnswers = request.userAnswers
    val memberList = userAnswers.membersDetails(srn)

    if (memberList.size > Constants.maxSchemeMembers) {
      Redirect(
        navigator.nextPage(ReportMemberContributionListPage(srn), mode, request.userAnswers)
      )
    } else {

      form
        .bindFromRequest()
        .fold(
          errors =>
            BadRequest(
              view(errors, ReportMemberContributionListController.viewModel(srn, page, mode, memberList, userAnswers))
            ),
          _ =>
            Redirect(
              navigator
                .nextPage(ReportMemberContributionListPage(srn), mode, request.userAnswers)
            )
        )
    }
  }
}

object ReportMemberContributionListController {
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
              ) ++ buildMutableTable(contributions, srn, nextIndex, mode)
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
      controllers.nonsipp.membercontributions.routes.ReportMemberContributionListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = Some(ParagraphMessage("ReportContribution.MemberList.paragraph")),
      page = ActionTableViewModel(
        inset = "ReportContribution.MemberList.inset",
        head = Some(List(TableElem("Member name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("ReportContribution.MemberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
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
        controllers.nonsipp.membercontributions.routes.ReportMemberContributionListController.onSubmit(srn, page, mode)
    )
  }

  private def buildMutableTable(
    contribs: Option[Money],
    srn: Srn,
    nextIndex: Refined[Int, OneTo300],
    mode: Mode
  ): List[TableElem] =
    List(
      TableElem(
        "Member contributions reported"
      ),
      TableElem(
        LinkMessage(
          "Change",
          controllers.nonsipp.membercontributions.routes.CYAMemberContributionsController
            .onPageLoad(srn, nextIndex, CheckOrChange.Change)
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
