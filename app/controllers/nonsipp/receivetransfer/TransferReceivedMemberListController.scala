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

package controllers.nonsipp.receivetransfer

import com.google.inject.Inject
import config.Constants
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions._
import eu.timepit.refined.{refineMV, refineV}
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models._
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.receivetransfer._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction

import javax.inject.Named

class TransferReceivedMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form = TransferReceivedMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = TransferReceivedMemberListController
          .viewModel(srn, page, mode, memberList, request.userAnswers)
        Ok(view(form, viewModel))
      } else {
        Redirect(
          controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
            .onSubmit(srn, refineMV(1), refineMV(2), mode)
        )
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val memberList = request.userAnswers.membersDetails(srn)

    if (memberList.size > Constants.maxSchemeMembers) {
      Redirect(
        navigator.nextPage(TransferReceivedMemberListPage(srn), mode, request.userAnswers)
      )
    } else {
      val viewModel =
        TransferReceivedMemberListController.viewModel(srn, page, mode, memberList, request.userAnswers)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          _ =>
            Redirect(
              navigator
                .nextPage(TransferReceivedMemberListPage(srn), mode, request.userAnswers)
            )
        )
    }
  }
}

object TransferReceivedMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "transferIn.MemberList.radios.error.required"
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
            val contributions = userAnswers.map(TransferringSchemeNamePages(srn, nextIndex))
            if (contributions.isEmpty) {
              List(
                TableElem(
                  memberName.fullName
                ),
                TableElem(
                  Message("transferIn.MemberList.status.no.contributions")
                ),
                TableElem(
                  LinkMessage(
                    Message("site.add"),
                    controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
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
                    Message("transferIn.MemberList.singleStatus.some.contribution", contributions.size)
                  else
                    Message("transferIn.MemberList.status.some.contributions", contributions.size)
                ),
                TableElem(
                  LinkMessage(
                    Message("site.change"),
                    controllers.routes.UnauthorisedController.onPageLoad().url
                  )
                ),
                TableElem(
                  LinkMessage(
                    Message("site.remove"),
                    controllers.routes.UnauthorisedController.onPageLoad().url
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
      if (memberList.size == 1) "transferIn.MemberList.title"
      else "transferIn.MemberList.title.plural"

    val heading =
      if (memberList.size == 1) "transferIn.MemberList.heading"
      else "transferIn.MemberList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.transferInListSize,
      memberList.size,
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = Some(ParagraphMessage("transferIn.MemberList.paragraph")),
      page = ActionTableViewModel(
        inset = "transferIn.MemberList.inset",
        head = Some(List(TableElem("Member Name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("transferIn.MemberList.radios"),
        showRadios = memberList.length < 9999999,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "transferIn.MemberList.pagination.label",
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
      onSubmit = controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onSubmit(srn, page, mode)
    )
  }
}
