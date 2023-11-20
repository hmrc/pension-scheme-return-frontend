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

package controllers.nonsipp.memberpayments

import com.google.inject.Inject
import config.{Constants, FrontendAppConfig}
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions._
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.CheckOrChange.Change
import models.SchemeId.Srn
import models._
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberpayments.TransferReceivedMemberListPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction
import viewmodels.implicits._

import javax.inject.Named

class TransferReceivedMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  appConfig: FrontendAppConfig,
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
          .viewModel(srn, page, mode, memberList)
        Ok(view(form, viewModel))
      } else {
        Redirect(controllers.routes.UnauthorisedController.onPageLoad())
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
        TransferReceivedMemberListController.viewModel(srn, page, mode, memberList)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer =>
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
      "TransferIn.MemberList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[NameDOB]
  ): List[List[TableElem]] =
    memberList.zipWithIndex.map {
      case (memberName, index) =>
        refineV[OneTo300](index + 1) match {
          case Left(_) => Nil
          case Right(nextIndex) =>
            List(
              TableElem(
                memberName.fullName
              ),
              TableElem(
                "No transfers in" //TODO We need to complete the "Add" Journey to be able to make this dynamic
              ),
              TableElem(
                LinkMessage("Add", controllers.routes.UnauthorisedController.onPageLoad().url) //TODO we need the subsequent page in the Journey to add the right link
              )
            )
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[NameDOB]
  ): FormPageViewModel[ActionTableViewModel] = {

    val title = if (memberList.size == 1) "TransferIn.MemberList.title" else "TransferIn.MemberList.title.plural"
    val heading = if (memberList.size == 1) "TransferIn.MemberList.heading" else "TransferIn.MemberList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertiesSize,
      memberList.size,
      controllers.nonsipp.memberpayments.routes.TransferReceivedMemberListController.onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = Some(ParagraphMessage("TransferIn.MemberList.paragraph")),
      page = ActionTableViewModel(
        inset = "TransferIn.MemberList.inset",
        head = Some(List(TableElem("Member Name"), TableElem("status"))),
        rows = rows(srn, mode, memberList),
        radioText = Message("TransferIn.MemberList.radios"),
        showRadios = memberList.length < 9999999,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "TransferIn.MemberList.pagination.label",
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
        controllers.nonsipp.memberpayments.routes.TransferReceivedMemberListController.onSubmit(srn, page, mode)
    )
  }
}
