/*
 * Copyright 2024 HM Revenue & Customs
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

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MembersDetailsPages
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.OneTo300
import controllers.PSRController
import config.Constants
import pages.nonsipp.receivetransfer._
import config.Constants.maxNotRelevant
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import play.api.i18n.MessagesApi
import play.api.data.Form
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.{refineMV, refineV}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class TransferReceivedMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  saveService: SaveService,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = TransferReceivedMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val memberMap = request.userAnswers.map(MembersDetailsPages(srn))
      val maxIndex: Either[Result, Int] = memberMap.keys
        .map(_.toInt)
        .maxOption
        .map(Right(_))
        .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val optionList: List[Option[NameDOB]] = maxIndex match {
        case Right(index) =>
          (0 to index).toList.map { index =>
            val memberOption = memberMap.get(index.toString)
            memberOption match {
              case Some(member) => Some(member)
              case None => None
            }
          }
        case Left(_) => List.empty
      }

      if (memberMap.nonEmpty) {
        val viewModel = TransferReceivedMemberListController
          .viewModel(srn, page, mode, optionList, request.userAnswers)
        val filledForm =
          request.userAnswers.get(TransferReceivedMemberListPage(srn)).fold(form)(form.fill)
        Ok(view(filledForm, viewModel))
      } else {
        Redirect(controllers.routes.UnauthorisedController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val memberMap = request.userAnswers.map(MembersDetailsPages(srn))
      val maxIndex: Either[Result, Int] = memberMap.keys
        .map(_.toInt)
        .maxOption
        .map(Right(_))
        .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val optionList: List[Option[NameDOB]] = maxIndex match {
        case Right(index) =>
          (0 to index).toList.map { index =>
            val memberOption = memberMap.get(index.toString)
            memberOption match {
              case Some(member) => Some(member)
              case None => None
            }
          }
        case Left(_) => List.empty
      }

      if (optionList.flatten.size > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(TransferReceivedMemberListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        val viewModel =
          TransferReceivedMemberListController.viewModel(srn, page, mode, optionList, request.userAnswers)

        form
          .bindFromRequest()
          .fold(
            errors => Future.successful(BadRequest(view(errors, viewModel))),
            finishedAddingTransfers =>
              for {
                updatedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .set(
                        TransfersInJourneyStatus(srn),
                        if (finishedAddingTransfers) SectionStatus.Completed
                        else SectionStatus.InProgress
                      )
                      .set(TransferReceivedMemberListPage(srn), finishedAddingTransfers)
                  )
                _ <- saveService.save(updatedUserAnswers)
                _ <- if (finishedAddingTransfers)
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    optFallbackCall = Some(
                      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
                        .onPageLoad(srn, page, mode)
                    )
                  )
                else Future.successful(Some(()))
              } yield Redirect(
                navigator
                  .nextPage(TransferReceivedMemberListPage(srn), mode, updatedUserAnswers)
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
    memberList: List[Option[NameDOB]],
    userAnswers: UserAnswers
  ): List[List[TableElem]] =
    memberList.zipWithIndex
      .map {
        case (Some(memberName), index) =>
          refineV[OneTo300](index + 1) match {
            case Left(_) => Nil
            case Right(nextIndex) =>
              val contributions = userAnswers.map(TransfersInSectionCompletedForMember(srn, nextIndex))
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
                      controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
                        .onSubmit(srn, nextIndex, CheckMode)
                        .url
                    )
                  ),
                  TableElem(
                    LinkMessage(
                      Message("site.remove"),
                      controllers.nonsipp.receivetransfer.routes.WhichTransferInRemoveController
                        .onSubmit(srn, nextIndex)
                        .url
                    )
                  )
                )
              }
          }
        case _ => List.empty
      }
      .sortBy(_.headOption.map(_.text.toString))

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[Option[NameDOB]],
    userAnswers: UserAnswers
  ): FormPageViewModel[ActionTableViewModel] = {
    val title =
      if (memberList.flatten.size == 1) "transferIn.MemberList.title"
      else "transferIn.MemberList.title.plural"

    val heading =
      if (memberList.flatten.size == 1) "transferIn.MemberList.heading"
      else "transferIn.MemberList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.transferInListSize,
      memberList.flatten.size,
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.flatten.size),
      heading = Message(heading, memberList.flatten.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage(
          "transferIn.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "transferIn.MemberList.paragraph2"
          ),
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("transferIn.MemberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
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
