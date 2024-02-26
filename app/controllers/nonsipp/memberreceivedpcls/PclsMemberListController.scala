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

import cats.implicits.{toBifunctorOps, toTraverseOps}
import com.google.inject.Inject
import config.Constants
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions._
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models._
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberreceivedpcls._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class PclsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = PclsMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val ua = request.userAnswers
      val memberList = ua.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = PclsMemberListController
          .viewModel(srn, page, mode, memberList, ua)
        val filledForm =
          request.userAnswers.get(PclsMemberListPage(srn)).fold(form)(form.fill)
        Ok(view(filledForm, viewModel))
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val ua = request.userAnswers
      val memberList = ua.membersDetails(srn)
      val memberListSize = memberList.size

      if (memberListSize > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(PclsMemberListPage(srn), mode, ua)
          )
        )
      } else {

        form
          .bindFromRequest()
          .fold(
            errors =>
              Future.successful(
                BadRequest(
                  view(errors, PclsMemberListController.viewModel(srn, page, mode, memberList, ua))
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
                      .nextPage(PclsMemberListPage(srn), mode, request.userAnswers)
                  )
              )
          )
      }
  }

  private def buildUserAnswerBySelection(srn: Srn, selection: Boolean, memberListSize: Int)(
    implicit request: DataRequest[_]
  ): Future[UserAnswers] = {
    val userAnswerWithPclsMemberList = request.userAnswers.set(PclsMemberListPage(srn), selection)

    if (selection) {
      val indexes = (1 to memberListSize)
        .map(i => refineV[OneTo300](i).leftMap(new Exception(_)).toTry)
        .toList
        .sequence

      Future.fromTry(
        indexes.fold(
          _ => userAnswerWithPclsMemberList,
          index =>
            index.foldLeft(userAnswerWithPclsMemberList) {
              case (uaTry, index) =>
                val optCommencementLumpSumAmount =
                  request.userAnswers.get(PensionCommencementLumpSumAmountPage(srn, index))
                for {
                  ua <- uaTry
                  ua1 <- ua.set(
                    PensionCommencementLumpSumAmountPage(srn, index),
                    optCommencementLumpSumAmount.getOrElse(PensionCommencementLumpSum(Money(0), Money(0)))
                  )
                } yield ua1
            }
        )
      )
    } else {
      Future.fromTry(userAnswerWithPclsMemberList)
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
    memberList: List[NameDOB],
    userAnswers: UserAnswers
  ): List[List[TableElem]] =
    memberList.zipWithIndex.map {
      case (memberName, index) =>
        refineV[OneTo300](index + 1) match {
          case Left(_) => Nil
          case Right(nextIndex) =>
            val items = userAnswers.get(PensionCommencementLumpSumAmountPage(srn, nextIndex))
            if (items.isEmpty || items.exists(_.isZero)) {
              List(
                TableElem(
                  memberName.fullName
                ),
                TableElem(
                  Message("pcls.memberlist.status.no.items")
                ),
                TableElem.add(
                  controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                    .onSubmit(srn, nextIndex, NormalMode)
                ),
                TableElem.empty
              )
            } else {
              List(
                TableElem(
                  memberName.fullName
                ),
                TableElem(
                  Message("pcls.memberlist.status.some.item", items.size)
                ),
                TableElem.change(
                  controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                    .onSubmit(srn, nextIndex, CheckMode)
                ),
                TableElem.remove(
                  controllers.nonsipp.memberreceivedpcls.routes.RemovePclsController
                    .onSubmit(srn, nextIndex)
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
        inset = ParagraphMessage(
          "pcls.memberlist.paragraph1"
        ) ++
          ParagraphMessage(
            "pcls.memberlist.paragraph2"
          ),
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, memberList, userAnswers),
        radioText = Message("pcls.memberlist.radios"),
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
