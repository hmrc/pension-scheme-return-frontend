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

package controllers.nonsipp.memberpensionpayments

import com.google.inject.Inject
import config.Constants
import config.Constants.maxNotRelevant
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models._
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberpensionpayments.MemberPensionPaymentsListPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction

import javax.inject.Named

class MemberPensionPaymentsListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  private val form = MemberPensionPaymentsListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = MemberPensionPaymentsListController
          .viewModel(srn, page, mode, memberList, request.userAnswers)
        Ok(view(form, viewModel))
      } else {
        Redirect(
          controllers.routes.UnauthorisedController.onPageLoad()
        )
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val memberList = request.userAnswers.membersDetails(srn)

    if (memberList.size > Constants.maxSchemeMembers) {
      Redirect(
        navigator.nextPage(MemberPensionPaymentsListPage(srn), mode, request.userAnswers)
      )
    } else {
      val viewModel =
        MemberPensionPaymentsListController.viewModel(srn, page, mode, memberList, request.userAnswers)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          _ =>
            Redirect(
              navigator
                .nextPage(MemberPensionPaymentsListPage(srn), mode, request.userAnswers)
            )
        )
    }
  }
}

object MemberPensionPaymentsListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "memberPensionPayments.memberList.radios.error.required"
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
            val contributions = userAnswers.get(MemberPensionPaymentsListPage(srn))
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
        ("memberPensionPayments.memberList.title", "memberPensionPayments.memberList.heading")
      } else {
        ("memberPensionPayments.memberList.title.plural", "memberPensionPayments.memberList.heading.plural")
      }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.memberPensionPayments,
      memberList.size,
      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage(
          "memberPensionPayments.memberList.paragraphOne"
        ) ++
          ParagraphMessage(
            "memberPensionPayments.memberList.paragraphTwo"
          ),
        head = Some(List(TableElem("Member name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("memberPensionPayments.memberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "memberPensionPayments.memberList.pagination.label",
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
        controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController.onSubmit(srn, page, mode)
    )
  }

  private def buildMutableTable(
    srn: Srn,
    nextIndex: Refined[Int, OneTo300]
  ): List[TableElem] =
    List(
      TableElem(
        "memberPensionPayments.memberList.pensionPaymentsReported"
      ),
      TableElem(
        LinkMessage(
          "Change",
          controllers.routes.UnauthorisedController
            .onPageLoad()
            .url
        )
      ),
      TableElem(
        LinkMessage(
          "Remove",
          controllers.routes.UnauthorisedController
            .onPageLoad()
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
        "memberPensionPayments.memberList.noPensionPayments"
      ),
      TableElem(
        LinkMessage(
          "Add",
          controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
            .onSubmit(srn, nextIndex, mode)
            .url
        )
      ),
      TableElem("")
    )
}
