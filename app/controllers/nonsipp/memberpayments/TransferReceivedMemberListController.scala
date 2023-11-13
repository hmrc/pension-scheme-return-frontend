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
import config.Constants
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.memberdetails.routes
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
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel, PaginatedViewModel}
import views.html.ListView
import viewmodels.implicits._

import javax.inject.Named

class TransferReceivedMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form = TransferReceivedMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = TransferReceivedMemberListController.viewModel(srn, page, mode, memberList)
        Ok(view(form, viewModel))
      } else {
        Redirect(controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, mode))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val memberList = request.userAnswers.membersDetails(srn)

    if (memberList.size == Constants.maxSchemeMembers) {
      Redirect(
        navigator.nextPage(TransferReceivedMemberListPage(srn, addLandOrProperty = false), mode, request.userAnswers)
      )
    } else {
      val viewModel = TransferReceivedMemberListController.viewModel(srn, page, mode, memberList)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer =>
            Redirect(
              navigator
                .nextPage(TransferReceivedMemberListPage(srn, addLandOrProperty = answer), mode, request.userAnswers)
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

  private def rows(srn: Srn, mode: Mode, memberList: List[NameDOB]): List[ListRow] =
    memberList.zipWithIndex.flatMap {
      case (memberName, index) =>
        refineV[OneTo300](index + 1) match {
          case Left(_) => Nil
          case Right(nextIndex) =>
            List(
              ListRow(
                memberName.fullName,
                changeUrl = routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, nextIndex, Change).url,
                changeHiddenText = Message("schemeMembersList.change.hidden", memberName.fullName),
                removeUrl = routes.RemoveMemberDetailsController.onPageLoad(srn, nextIndex, mode).url,
                removeHiddenText = Message("schemeMembersList.remove.hidden", memberName.fullName)
              )
            )
        }
    }

  def viewModel(srn: Srn, page: Int, mode: Mode, memberList: List[NameDOB]): FormPageViewModel[ListViewModel] = {

    val title = if (memberList.size == 1) "TransferIn.MemberList.title" else "TransferIn.MemberList.title.plural"
    val heading = if (memberList.size == 1) "TransferIn.MemberList.heading" else "TransferIn.MemberList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertiesSize,
      memberList.size,
      controllers.nonsipp.memberpayments.routes.TransferReceivedMemberListController.onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      Message(title, memberList.size),
      Message(heading, memberList.size),
      ParagraphMessage("TransferIn.MemberList.paragraph"),
      ListViewModel(
        inset = "TransferIn.MemberList.inset",
        rows(srn, mode, memberList),
        Message("TransferIn.MemberList.radios"),
        showRadios = memberList.size < Constants.maxSchemeMembers,
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
      controllers.nonsipp.memberpayments.routes.TransferReceivedMemberListController.onSubmit(srn, page, mode)
    )
  }
}
