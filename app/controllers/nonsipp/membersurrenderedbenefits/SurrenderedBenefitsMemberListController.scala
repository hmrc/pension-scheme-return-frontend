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

package controllers.nonsipp.membersurrenderedbenefits

import com.google.inject.Inject
import config.Constants
import config.Constants.maxNotRelevant
import config.Refined.OneTo300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, NameDOB, NormalMode, Pagination, UserAnswers}
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsMemberListPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ActionTableViewModel, FormPageViewModel, PaginatedViewModel, TableElem}
import views.html.TwoColumnsTripleAction

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class SurrenderedBenefitsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form = SurrenderedBenefitsMemberListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val userAnswers = request.userAnswers
      val memberList = userAnswers.membersDetails(srn)

      if (memberList.nonEmpty) {
        val viewModel = SurrenderedBenefitsMemberListController
          .viewModel(srn, page, mode, memberList, userAnswers)
        val filledForm =
          userAnswers.get(SurrenderedBenefitsMemberListPage(srn)).fold(form)(form.fill)
        Ok(view(filledForm, viewModel))
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val userAnswers = request.userAnswers
      val memberList = userAnswers.membersDetails(srn)
      val memberListSize = memberList.size

      if (memberListSize > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(SurrenderedBenefitsMemberListPage(srn), mode, userAnswers)
          )
        )
      } else {
        val viewModel =
          SurrenderedBenefitsMemberListController.viewModel(srn, page, mode, memberList, userAnswers)

        form
          .bindFromRequest()
          .fold(
            errors =>
              Future.successful(
                BadRequest(
                  view(errors, viewModel)
                )
              ),
            value =>
              for {
                updatedUserAnswers <- buildUserAnswersBySelection(srn, value, memberListSize)
                _ <- saveService.save(updatedUserAnswers)
                //TODO: update once Surrendered Benefits journey is complete/as part of transformation work
                submissionResult <- Future.successful(Some(()))
//              submissionResult <- if (value)
//              {
//                psrSubmissionService.submitPsrDetails(srn)(
//                  implicitly,
//                  implicitly,
//                  request = DataRequest(request.request, updatedUserAnswers)
//                )
//              }
//              else {
//                Future.successful(Some(()))
//              }
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

  private def buildUserAnswersBySelection(srn: Srn, selection: Boolean, memberListSize: Int)(
    implicit request: DataRequest[_]
  ): Future[UserAnswers] = {
    val userAnswersWithSurrenderedBenefitsMemberList =
      request.userAnswers.set(SurrenderedBenefitsMemberListPage(srn), selection)

    //TODO: complete once Surrendered Benefits journey is complete/as part of transformation work
    Future.fromTry(userAnswersWithSurrenderedBenefitsMemberList)
//    if (selection) {
//      val indexes = (1 to memberListSize)
//        .map(i => refineV[OneTo300](i).leftMap(new Exception(_)).toTry)
//        .toList
//        .sequence
//
//      Future.fromTry(
//        indexes.fold(
//          _ => userAnswersWithSurrenderedBenefitsMemberList,
//          index =>
//            index.foldLeft(userAnswersWithSurrenderedBenefitsMemberList) {
//              case (userAnswersTry, index) =>
//                val optSurrenderedBenefitsAmount = request.userAnswers.get(SurrenderedBenefitsAmountPage(srn, index))
//                for {
//                  ua <- userAnswersTry
//                  ua1 <- ua.set(SurrenderedBenefitsAmountPage(srn, index), optSurrenderedBenefitsAmount.getOrElse(Money(0)))
//                } yield ua1
//            }
//        )
//      )
//    } else {
//      Future.fromTry(userAnswersWithSurrenderedBenefitsMemberList)
//    }
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
    memberList: List[NameDOB],
    userAnswers: UserAnswers
  ): List[List[TableElem]] =
    memberList.zipWithIndex.map {
      case (memberName, index) =>
        refineV[OneTo300](index + 1) match {
          case Left(_) => Nil
          case Right(nextIndex) =>
            //TODO: update this once SurrenderedBenefitsAmount has been implemented
            val items = Map[String, String]().empty
//            val items = userAnswers.map(SurrenderedBenefitsAmountPage(srn, nextIndex))
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
                    //TODO: update this once SurrenderedBenefitsAmount has been implemented
                    controllers.routes.UnauthorisedController.onPageLoad().url
//                    controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsAmountController
//                      .onSubmit(srn, nextIndex, mode)
//                      .url
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
                    //TODO: update this once SurrenderedBenefitsCYA has been implemented
                    controllers.routes.UnauthorisedController.onPageLoad().url
//                    controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsAmountController
//                      .onSubmit(srn, nextIndex, CheckMode)
//                      .url
                  )
                ),
                TableElem(
                  LinkMessage(
                    Message("site.remove"),
                    //TODO: update this once RemoveSurrenderedBenefits has been implemented
                    controllers.routes.UnauthorisedController.onPageLoad().url
//                    controllers.nonsipp.membersurrenderedbenefits.routes.RemoveSurrenderedBenefitsController
//                      .onSubmit(srn, nextIndex)
//                      .url
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
    val title = "surrenderedBenefits.memberList.title"
    val heading = "surrenderedBenefits.memberList.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.surrenderedBenefitsListSize,
      memberList.size,
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage("surrenderedBenefits.memberList.inset1") ++
          ParagraphMessage("surrenderedBenefits.memberList.inset2"),
        head = Some(List(TableElem("Member name"), TableElem("Status"))),
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
