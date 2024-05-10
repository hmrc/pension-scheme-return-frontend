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

package controllers.nonsipp.membersurrenderedbenefits

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MembersDetailsPages
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.OneTo300
import controllers.PSRController
import config.Constants
import config.Constants.maxNotRelevant
import navigation.Navigator
import forms.YesNoPageFormProvider
import pages.nonsipp.membersurrenderedbenefits.{
  SurrenderedBenefitsAmountPage,
  SurrenderedBenefitsJourneyStatus,
  SurrenderedBenefitsMemberListPage
}
import models._
import play.api.i18n.MessagesApi
import play.api.data.Form
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class SurrenderedBenefitsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = SurrenderedBenefitsMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
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

      if (optionList.flatten.nonEmpty) {
        val viewModel = SurrenderedBenefitsMemberListController
          .viewModel(srn, page, mode, optionList, userAnswers)
        val filledForm =
          userAnswers.get(SurrenderedBenefitsMemberListPage(srn)).fold(form)(form.fill)
        Ok(view(filledForm, viewModel))
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
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
            navigator.nextPage(SurrenderedBenefitsMemberListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        val viewModel =
          SurrenderedBenefitsMemberListController.viewModel(srn, page, mode, optionList, request.userAnswers)

        form
          .bindFromRequest()
          .fold(
            errors => Future.successful(BadRequest(view(errors, viewModel))),
            finishedAddingSurrenderedBenefits =>
              for {
                updatedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .set(
                        SurrenderedBenefitsJourneyStatus(srn),
                        if (finishedAddingSurrenderedBenefits) {
                          SectionStatus.Completed
                        } else {
                          SectionStatus.InProgress
                        }
                      )
                      .set(SurrenderedBenefitsMemberListPage(srn), finishedAddingSurrenderedBenefits)
                  )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (finishedAddingSurrenderedBenefits) {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall =
                      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
                        .onPageLoad(srn, page, mode)
                  )
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(SurrenderedBenefitsMemberListPage(srn), mode, request.userAnswers)
                  )
              )
          )
      }
  }
}

object SurrenderedBenefitsMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "surrenderedBenefits.memberList.radios.error.required"
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
              val items = userAnswers.get(SurrenderedBenefitsAmountPage(srn, nextIndex))
              if (items.isEmpty) {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    Message("surrenderedBenefits.memberList.status.no.items")
                  ),
                  TableElem(
                    LinkMessage(
                      Message("site.add"),
                      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsAmountController
                        .onSubmit(srn, nextIndex, mode)
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
                    Message("surrenderedBenefits.memberList.status.some.item", items.size)
                  ),
                  TableElem(
                    LinkMessage(
                      Message("site.change"),
                      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
                        .onPageLoad(srn, nextIndex, CheckMode)
                        .url
                    )
                  ),
                  TableElem(
                    LinkMessage(
                      Message("site.remove"),
                      controllers.nonsipp.membersurrenderedbenefits.routes.RemoveSurrenderedBenefitsController
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
    val title = "surrenderedBenefits.memberList.title"
    val heading = "surrenderedBenefits.memberList.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.surrenderedBenefitsListSize,
      memberList.flatten.size,
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.flatten.size),
      heading = Message(heading, memberList.flatten.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage("surrenderedBenefits.memberList.inset1") ++
          ParagraphMessage("surrenderedBenefits.memberList.inset2"),
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, userAnswers),
        radioText = Message("surrenderedBenefits.memberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "surrenderedBenefits.memberList.pagination.label",
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
      onSubmit = controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onSubmit(srn, page, mode)
    )
  }
}
