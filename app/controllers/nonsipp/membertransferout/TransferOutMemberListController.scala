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

package controllers.nonsipp.membertransferout

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
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePages, TransferOutMemberListPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.implicits._
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction

import javax.inject.Named

class TransferOutMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form = TransferOutMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = TransferOutMemberListController
          .viewModel(srn, page, mode, memberList, request.userAnswers)
        Ok(view(form, viewModel))
      } else {
        Redirect(
          controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
            .onSubmit(srn, refineMV(1), refineMV(1), NormalMode)
        )
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val memberList = request.userAnswers.membersDetails(srn)

    if (memberList.size > Constants.maxSchemeMembers) {
      Redirect(
        navigator.nextPage(TransferOutMemberListPage(srn), mode, request.userAnswers)
      )
    } else {
      val viewModel =
        TransferOutMemberListController.viewModel(srn, page, mode, memberList, request.userAnswers)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          _ =>
            Redirect(
              navigator
                .nextPage(TransferOutMemberListPage(srn), mode, request.userAnswers)
            )
        )
    }
  }
}

object TransferOutMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "transferOut.memberList.radios.error.required"
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
            val contributions = userAnswers.map(ReceivingSchemeNamePages(srn, nextIndex))
            if (contributions.isEmpty) {
              List(
                TableElem(
                  memberName.fullName
                ),
                TableElem(
                  Message("transferOut.memberList.status.no.contributions")
                ),
                TableElem(
                  LinkMessage(
                    Message("site.add"),
                    controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
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
                    Message("transferOut.memberList.singleStatus.some.contribution", contributions.size)
                  else
                    Message("transferOut.memberList.status.some.contributions", contributions.size)
                ),
                TableElem(
                  LinkMessage(
                    Message("site.change"),
                    controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
                      .onPageLoad(srn, nextIndex, CheckMode)
                      .url
                  )
                ),
                TableElem(
                  LinkMessage(
                    Message("site.remove"),
                    controllers.nonsipp.membertransferout.routes.WhichTransferOutRemoveController
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
      if (memberList.size == 1) "transferOut.memberList.title"
      else "transferOut.memberList.title.plural"

    val heading =
      if (memberList.size == 1) "transferOut.memberList.heading"
      else "transferOut.memberList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.transferOutListSize,
      memberList.size,
      controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = None,
      page = ActionTableViewModel(
        inset = "transferOut.memberList.paragraph",
        head = Some(List(TableElem("Member name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("transferOut.memberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "transferOut.memberList.pagination.label",
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
      onSubmit = controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
        .onSubmit(srn, page, mode)
    )
  }
}
