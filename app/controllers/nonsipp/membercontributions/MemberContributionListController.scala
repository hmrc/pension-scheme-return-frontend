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

package controllers.nonsipp.membercontributions

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MembersDetailsPages
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.OneTo300
import controllers.PSRController
import config.Constants
import controllers.nonsipp.membercontributions.MemberContributionListController._
import cats.implicits.{toBifunctorOps, toTraverseOps}
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import play.api.i18n.MessagesApi
import viewmodels.implicits._
import pages.nonsipp.membercontributions.{MemberContributionsListPage, TotalMemberContributionPage}
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class MemberContributionListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = MemberContributionListController.form(formProvider)

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
        val filledForm = request.userAnswers.get(MemberContributionsListPage(srn)).fold(form)(form.fill)
        Ok(view(filledForm, viewModel(srn, page, mode, optionList, request.userAnswers)))
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val userAnswers = request.userAnswers
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
            navigator.nextPage(MemberContributionsListPage(srn), mode, request.userAnswers)
          )
        )
      } else {

        form
          .bindFromRequest()
          .fold(
            errors =>
              Future.successful(
                BadRequest(
                  view(
                    errors,
                    MemberContributionListController.viewModel(srn, page, mode, optionList, userAnswers)
                  )
                )
              ),
            value =>
              for {
                updatedUserAnswers <- buildUserAnswerBySelection(srn, value, optionList.flatten.size)
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (value) {
                  psrSubmissionService.submitPsrDetailsWithUA(srn, updatedUserAnswers)
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(MemberContributionsListPage(srn), mode, request.userAnswers)
                  )
              )
          )
      }
  }

  private def buildUserAnswerBySelection(srn: Srn, selection: Boolean, memberListSize: Int)(
    implicit request: DataRequest[_]
  ): Future[UserAnswers] = {
    val userAnswerWithMemberContList = request.userAnswers.set(MemberContributionsListPage(srn), selection)

    if (selection) {
      val indexes = (1 to memberListSize)
        .map(i => refineV[OneTo300](i).leftMap(new Exception(_)).toTry)
        .toList
        .sequence

      Future.fromTry(
        indexes.fold(
          _ => userAnswerWithMemberContList,
          index =>
            index.foldLeft(userAnswerWithMemberContList) {
              case (uaTry, index) =>
                val optTotalMemberContribution = request.userAnswers.get(TotalMemberContributionPage(srn, index))
                for {
                  ua <- uaTry
                  ua1 <- ua.set(TotalMemberContributionPage(srn, index), optTotalMemberContribution.getOrElse(Money(0)))
                } yield ua1
            }
        )
      )
    } else {
      Future.fromTry(userAnswerWithMemberContList)
    }
  }
}

object MemberContributionListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "ReportContribution.MemberList.radios.error.required"
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
            case Right(index) =>
              val contributions = userAnswers.get(TotalMemberContributionPage(srn, index))
              if (contributions.nonEmpty && !contributions.exists(_.isZero)) {
                List(
                  TableElem(memberName.fullName),
                  TableElem("Member contributions reported"),
                  TableElem.change(
                    controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
                      .onPageLoad(srn, index, CheckMode)
                  ),
                  TableElem.remove(
                    controllers.nonsipp.membercontributions.routes.RemoveMemberContributionController
                      .onPageLoad(srn, index)
                  )
                )
              } else {
                List(
                  TableElem(memberName.fullName),
                  TableElem("No member contributions"),
                  TableElem.add(
                    controllers.nonsipp.membercontributions.routes.TotalMemberContributionController
                      .onSubmit(srn, index, mode)
                  ),
                  TableElem.empty
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

    val (title, heading) =
      if (memberList.flatten.size == 1) {
        ("ReportContribution.MemberList.title", "ReportContribution.MemberList.heading")
      } else {
        ("ReportContribution.MemberList.title.plural", "ReportContribution.MemberList.heading.plural")
      }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertiesSize,
      memberList.flatten.size,
      controllers.nonsipp.membercontributions.routes.MemberContributionListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.flatten.size),
      heading = Message(heading, memberList.flatten.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage(
          "ReportContribution.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "ReportContribution.MemberList.paragraph2"
          ),
        showInsetWithRadios = true,
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("ReportContribution.MemberList.radios"),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "ReportContribution.MemberList.pagination.label",
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
        controllers.nonsipp.membercontributions.routes.MemberContributionListController.onSubmit(srn, page, mode)
    )
  }
}
