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

import cats.implicits.toTraverseOps
import com.google.inject.Inject
import config.{Constants, FrontendAppConfig}
import config.Refined.{Max300, Max50, OneTo300}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.employercontributions.EmployerContributionsMemberListController.viewModel
import eu.timepit.refined.api.Refined
import eu.timepit.refined.{refineMV, refineV}
import forms.YesNoPageFormProvider
import models.CheckOrChange.Change
import models.SchemeId.Srn
import models._
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.employercontributions.{EmployerContributionsMemberListPage, TotalEmployerContributionPages}
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction
import viewmodels.implicits._

import javax.inject.Named

class EmployerContributionsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  appConfig: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form = EmployerContributionsMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)
      val myList: Either[Result, List[MembersAndContributions]] =
        buildMembersAndContributions(srn, memberList, request.userAnswers)
      myList.map { memberData =>
        if (memberData.nonEmpty) {
          Ok(view(form, viewModel(srn, page = 1, mode, memberData)))

        } else {
          Redirect(
            controllers.nonsipp.employercontributions.routes.EmployerNameController
              .onSubmit(srn, refineMV(1), refineMV(2), mode)
          )
        }
      }.merge
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val memberList = request.userAnswers.membersDetails(srn)
    val myList: Either[Result, List[MembersAndContributions]] =
      buildMembersAndContributions(srn, memberList, request.userAnswers)
    myList.map { memberData =>
      if (memberData.isEmpty) {
        Redirect(
          navigator.nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
        )
      } else {
        val viewModel =
          EmployerContributionsMemberListController.viewModel(srn, page, mode, memberData)

        form
          .bindFromRequest()
          .fold(
            errors => BadRequest(view(errors, viewModel)),
            answer =>
              Redirect(
                navigator
                  .nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
              )
          )
      }
    }.merge
  }

  private def buildMembersAndContributions(
    srn: Srn,
    memberList: List[NameDOB],
    request: UserAnswers
  ): Either[Result, List[MembersAndContributions]] =
    memberList.zipWithIndex.traverse {
      case (memberName, index) =>
        refineV[OneTo300](index + 1) match {
          case Left(_) => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case Right(refinedIndex) => {
            val res = request.get(TotalEmployerContributionPages(srn, refinedIndex)).map(_.keys.toList)
            res match {

              case Some(value) => {
                val maybeContribs = value.traverse(_.toIntOption.map(refineV[Max50.Refined](_))).toList.flatten.sequence

                maybeContribs match {
                  case Right(value) => Right(MembersAndContributions(refinedIndex, memberName, value))
                  case Left(_) => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                }
              }
              case _ => Right(MembersAndContributions(refinedIndex, memberName, Nil))
            }
          }
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
    memberList: List[MembersAndContributions]
  ): List[List[TableElem]] =
    memberList.map { aMember =>
      List(
        TableElem(
          aMember.data.fullName
        ),
        TableElem(
          "No employer contributions" //TODO We need to complete the "Add" Journey to be able to make this dynamic
        )
      ) ++ mutableAction(srn, aMember, mode)
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[MembersAndContributions]
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
        head = Some(List(TableElem("Member Name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList),
        radioText = Message("employerContributions.MemberList.radios"),
        showRadios = memberList.length < 9999999,
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

  def mutableAction(
    srn: Srn,
    member: MembersAndContributions,
    mode: Mode
  ): List[TableElem] =
    if (member.contribs.size > 0) {
      List(
        TableElem(
          LinkMessage(
            "Change",
            controllers.routes.UnauthorisedController
              .onPageLoad()
              .url //TODO once PSR-375 is done redirect to the PSR-375 page
          )
        ),
        TableElem(
          LinkMessage(
            "Remove",
            controllers.routes.UnauthorisedController.onPageLoad().url //TODO once we got the Remove pageS
          )
        )
      )
    } else {
      List(addAction(srn, member.index, mode))
    }

  def addAction(srn: Srn, nextIndex: Refined[Int, OneTo300], mode: Mode): TableElem =
    TableElem(
      LinkMessage(
        "Add",
        controllers.nonsipp.employercontributions.routes.EmployerNameController
          .onSubmit(srn, nextIndex, refineMV(1), mode)
          .url
      ) //TODO we need the subsequent page in the Journey to add the right link
    )

}

case class MembersAndContributions(index: Max300, data: NameDOB, contribs: List[Max50])
