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

package controllers.nonsipp.memberreceivedpcls

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
import pages.nonsipp.memberreceivedpcls._
import pages.nonsipp.receivetransfer._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.implicits._
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction

import javax.inject.Named

class PclsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  private val form = PclsMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberList = request.userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = PclsMemberListController
          .viewModel(srn, page, mode, memberList, request.userAnswers)
        Ok(view(form, viewModel))
      } else {
        Redirect(controllers.routes.UnauthorisedController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val memberList = request.userAnswers.membersDetails(srn)

    if (memberList.size > Constants.maxSchemeMembers) {
      Redirect(
        navigator.nextPage(PclsMemberListPage(srn), mode, request.userAnswers)
      )
    } else {
      val viewModel =
        PclsMemberListController.viewModel(srn, page, mode, memberList, request.userAnswers)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          _ =>
            Redirect(
              navigator
                .nextPage(PclsMemberListPage(srn), mode, request.userAnswers)
            )
        )
    }
  }
}

object PclsMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "pcls.memberlist.radios.error.required"
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
            val items = userAnswers.get(PensionCommencementLumpSumAmountPage(srn, nextIndex, NormalMode))
            if (items.isEmpty) {
              List(
                TableElem(
                  memberName.fullName
                ),
                TableElem(
                  Message("pcls.memberlist.status.no.items")
                ),
                TableElem(
                  LinkMessage(
                    Message("site.add"),
                    controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                      .onSubmit(srn, nextIndex, NormalMode)
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
                  Message("pcls.memberlist.status.some.item", items.size)
                ),
                TableElem(
                  LinkMessage(
                    Message("site.change"),
                    controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                      .onSubmit(srn, nextIndex, CheckMode)
                      .url
                  )
                ),
                TableElem(
                  LinkMessage(
                    Message("site.remove"),
//                    TODO change this when the PCLS removal page is done
//                    controllers.nonsipp.receivetransfer.routes.WhichTransferInRemoveController
//                      .onSubmit(srn, nextIndex)
//                      .url
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
    val title = "pcls.memberlist.title"
    val heading = "pcls.memberlist.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.pclsInListSize,
      memberList.size,
      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = None,
      page = ActionTableViewModel(
        inset = "pcls.memberlist.paragraph",
        head = Some(List(TableElem("Member name"), TableElem("Status"))),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("pcls.memberlist.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "pcls.memberlist.pagination.label",
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
      onSubmit = controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onSubmit(srn, page, mode)
    )
  }
}
