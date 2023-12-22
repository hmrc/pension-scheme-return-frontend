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

package controllers.nonsipp.employercontributions

import com.google.inject.Inject
import config.Constants
import config.Constants.maxNotRelevant
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions._
import eu.timepit.refined.{refineMV, refineV}
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models._
import navigation.Navigator
import pages.nonsipp.employercontributions.{
  EmployerContributionsMemberListPage,
  EmployerContributionsSectionStatus,
  EmployerNamePages
}
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

  val form: Form[Boolean] = EmployerContributionsMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = EmployerContributionsMemberListController
          .viewModel(srn, page, mode, memberList, request.userAnswers)
        val filledForm =
          request.userAnswers.get(EmployerContributionsMemberListPage(srn)).fold(form)(form.fill)
        Ok(view(filledForm, viewModel))
      } else {
        Redirect(
          controllers.routes.JourneyRecoveryController.onPageLoad()
        )
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)

      if (memberList.size > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
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
                    EmployerContributionsMemberListController
                      .viewModel(srn, page, mode, memberList, request.userAnswers)
                  )
                )
              ),
            value =>
              for {
                updatedUserAnswers <- Future.fromTry(
                  request.userAnswers
                    .set(
                      EmployerContributionsSectionStatus(srn),
                      if (value) SectionStatus.Completed
                      else SectionStatus.InProgress
                    )
                    .set(EmployerContributionsMemberListPage(srn), value)
                )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (value) psrSubmissionService.submitPsrDetails(srn, updatedUserAnswers)
                else Future.successful(Some(()))
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
}

object EmployerContributionsMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "employerContributions.MemberList.radios.error.required"
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
            val contributions = userAnswers.map(EmployerNamePages(srn, nextIndex))
            if (contributions.isEmpty) {
              List(
                TableElem(
                  memberName.fullName
                ),
                TableElem(
                  Message("employerContributions.MemberList.status.no.contributions")
                ),
                TableElem(
                  LinkMessage(
                    Message("site.add"),
                    controllers.nonsipp.employercontributions.routes.EmployerNameController
                      .onSubmit(srn, nextIndex, refineMV(1), mode)
                      .url
                  )
                ),
                TableElem("")
              )
            } else {
              List(
                TableElem(
                  memberName.fullName
                ),
                TableElem(
                  if (contributions.size == 1)
                    Message("employerContributions.MemberList.status.single.contribution", contributions.size)
                  else
                    Message("employerContributions.MemberList.status.some.contributions", contributions.size)
                ),
                TableElem(
                  LinkMessage(
                    Message("site.change"),
                    controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                      .onSubmit(srn, nextIndex, page = 1, CheckMode)
                      .url
                  )
                ),
                TableElem(
                  LinkMessage(
                    Message("site.remove"),
                    controllers.nonsipp.employercontributions.routes.WhichEmployerContributionRemoveController
                      .onSubmit(srn, nextIndex)
                      .url
                  )
                )
              )
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
    val title =
      if (memberList.size == 1) "employerContributions.MemberList.title"
      else "employerContributions.MemberList.title.plural"

    val heading =
      if (memberList.size == 1) "employerContributions.MemberList.heading"
      else "employerContributions.MemberList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.employerContributionsMemberListSize,
      memberList.size,
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = Some(ParagraphMessage("employerContributions.MemberList.paragraph")),
      page = ActionTableViewModel(
        inset = "employerContributions.MemberList.inset",
        head = Some(List(TableElem("Member name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("employerContributions.MemberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
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
}
