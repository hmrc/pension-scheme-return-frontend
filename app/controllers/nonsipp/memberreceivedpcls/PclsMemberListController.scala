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

package controllers.nonsipp.memberreceivedpcls

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MembersDetailsPages
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.OneTo300
import controllers.PSRController
import config.Constants
import cats.implicits.{toBifunctorOps, toTraverseOps}
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import play.api.i18n.MessagesApi
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls._
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

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

      if (optionList.flatten.nonEmpty) {
        val viewModel = PclsMemberListController
          .viewModel(srn, page, mode, optionList, ua)
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
                  view(errors, PclsMemberListController.viewModel(srn, page, mode, optionList, ua))
                )
              ),
            value =>
              for {
                updatedUserAnswers <- buildUserAnswerBySelection(srn, value, optionList.flatten.size)
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (value) {
                  psrSubmissionService.submitPsrDetails(
                    srn,
                    fallbackCall =
                      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController.onPageLoad(srn, page, mode)
                  )(
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
    memberList: List[Option[NameDOB]],
    userAnswers: UserAnswers
  ): List[List[TableElem]] =
    memberList.zipWithIndex
      .map {
        case (Some(memberName), index) =>
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
                      .onSubmit(srn, nextIndex, NormalMode),
                    Message("pcls.memberList.add.hidden.text", memberName.fullName)
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
                      .onSubmit(srn, nextIndex, CheckMode),
                    Message("pcls.memberList.change.hidden.text", memberName.fullName)
                  ),
                  TableElem.remove(
                    controllers.nonsipp.memberreceivedpcls.routes.RemovePclsController
                      .onSubmit(srn, nextIndex),
                    Message("pcls.memberList.remove.hidden.text", memberName.fullName)
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
    val title = "pcls.memberlist.title"
    val heading = "pcls.memberlist.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.pclsInListSize,
      memberList.flatten.size,
      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.flatten.size),
      heading = Message(heading, memberList.flatten.size),
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
