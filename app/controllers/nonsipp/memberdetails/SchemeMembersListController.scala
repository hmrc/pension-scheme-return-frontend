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

package controllers.nonsipp.memberdetails

import com.google.inject.Inject
import config.Constants
import config.Constants.maxSchemeMembers
import config.Refined.{Max300, OneTo300}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import eu.timepit.refined._
import forms.YesNoPageFormProvider
import models.CheckOrChange.Change
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{ManualOrUpload, Mode, NameDOB, Pagination}
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberStatusImplicits.MembersStatusOps
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberdetails.SchemeMembersListPage
import play.api.Logger
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.MapUtils.{MapOps, UserAnswersMapOps}
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.ListView

import javax.inject.Named

class SchemeMembersListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  private val logger: Logger = Logger(classOf[SchemeMembersListController])

  private def form(manualOrUpload: ManualOrUpload, maxNumberReached: Boolean = false): Form[Boolean] =
    SchemeMembersListController.form(formProvider, manualOrUpload, maxNumberReached)

  def onPageLoad(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val membersDetails = request.userAnswers.membersDetails(srn)
      if (membersDetails.isEmpty) {
        logger.error(s"no members found")
        Redirect(routes.PensionSchemeMembersController.onPageLoad(srn))
      } else {
        filterDeletedMembers(srn).map { filteredMembers =>
          val memberNames = filteredMembers.map { case (_, details, _) => details.fullName }
          Ok(
            view(
              form(manualOrUpload, filteredMembers.size >= maxSchemeMembers),
              viewModel(srn, page, manualOrUpload, mode, memberNames)
            )
          )
        }.merge
      }
    }

  def onSubmit(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val membersDetails = request.userAnswers.membersDetails(srn)
      form(manualOrUpload, membersDetails.size >= maxSchemeMembers)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            filterDeletedMembers(srn).map { filteredMembers =>
              val memberNames = filteredMembers.map { case (_, details, _) => details.fullName }
              BadRequest(
                view(
                  formWithErrors,
                  viewModel(srn, page, manualOrUpload, mode, memberNames)
                )
              )
            }
          }.merge,
          value => {
            if (membersDetails.size == maxSchemeMembers && value) {
              Redirect(routes.HowToUploadController.onPageLoad(srn))
            } else {
              Redirect(
                navigator.nextPage(SchemeMembersListPage(srn, value, manualOrUpload), mode, request.userAnswers)
              )
            }
          }
        )
    }

  // merges member details with member states and filters out any soft deleted members
  private def filterDeletedMembers(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, List[(Max300, NameDOB, MemberState)]] = {
    val maybeMembersDetails =
      request.userAnswers
        .membersDetails(srn)
        .zipWithIndexToMap
        .mapKeysToIndex[Max300.Refined]
        .getOrRecoverJourney
    val maybeMemberStates = request.userAnswers.memberStates(srn).mapKeysToIndex[Max300.Refined].getOrRecoverJourney

    maybeMembersDetails.flatMap { membersDetails =>
      maybeMemberStates.flatMap { memberStates =>
        if (membersDetails.size != memberStates.size) {
          logger.error(
            s"member details of size ${membersDetails.size} was not the same as size of member states ${memberStates.size}"
          )
          Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        } else {
          val sortedMemberDetails = membersDetails.sort { case (a, b) => a.value.max(b.value) }
          val sortedMemberStates = memberStates.sort { case (a, b) => a.value.max(b.value) }
          val zippedMembers: List[((Max300, NameDOB), (Max300, MemberState))] =
            sortedMemberDetails.zip(sortedMemberStates).toList
          val memberWithStates = zippedMembers.collect {
            case ((i1, details), (i2, state)) if i1.value == i2.value => Some((i1, details, state))
            case _ => None
          }.flatten

          if (memberWithStates.size != sortedMemberStates.size) {
            logger.error(
              s"zipping member details with states has resulted in a mismatch, this means the indexes didn't align after sorting"
            )
            Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          } else {
            Right(memberWithStates.iterator.filter { case (_, _, state) => state == MemberState.Active }.toList)
          }
        }
      }
    }
  }
}

object SchemeMembersListController {
  def form(
    formProvider: YesNoPageFormProvider,
    manualOrUpload: ManualOrUpload,
    maxNumberReached: Boolean = false
  ): Form[Boolean] = formProvider(
    manualOrUpload.fold(
      manual = if (maxNumberReached) "membersUploaded.error.required" else "schemeMembersList.error.required",
      upload = "membersUploaded.error.required"
    )
  )

  def viewModel(
    srn: Srn,
    page: Int,
    manualOrUpload: ManualOrUpload,
    mode: Mode,
    memberNames: List[String]
  ): FormPageViewModel[ListViewModel] = {

    val rows: List[ListRow] = memberNames.zipWithIndex.flatMap {
      case (memberName, index) =>
        refineV[OneTo300](index + 1) match {
          case Left(_) => Nil
          case Right(nextIndex) =>
            List(
              ListRow(
                memberName,
                changeUrl = routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, nextIndex, Change).url,
                changeHiddenText = Message("schemeMembersList.change.hidden", memberName),
                removeUrl = routes.RemoveMemberDetailsController.onPageLoad(srn, nextIndex, mode).url,
                removeHiddenText = Message("schemeMembersList.remove.hidden", memberName)
              )
            )
        }
    }

    val titleKey =
      if (memberNames.length > 1) "schemeMembersList.title.plural" else "schemeMembersList.title"
    val headingKey =
      if (memberNames.length > 1) "schemeMembersList.heading.plural" else "schemeMembersList.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.schemeMembersPageSize,
      rows.size,
      routes.SchemeMembersListController.onPageLoad(srn, _, manualOrUpload)
    )

    val radioText = manualOrUpload.fold(
      upload = "membersUploaded.radio",
      manual =
        if (memberNames.length < Constants.maxSchemeMembers) "schemeMembersList.radio" else "membersUploaded.radio"
    )
    val yesHintText = manualOrUpload.fold(
      upload = Some(Message("membersUploaded.radio.yes.hint")),
      manual =
        if (memberNames.length < Constants.maxSchemeMembers) None else Some(Message("membersUploaded.radio.yes.hint"))
    )

    FormPageViewModel(
      Message(titleKey, memberNames.length),
      Message(headingKey, memberNames.length),
      ListViewModel(
        inset = "schemeMembersList.inset",
        rows,
        radioText,
        showRadios = if (memberNames.length < Constants.maxSchemeMembers) {
          memberNames.length < Constants.maxSchemeMembers
        } else {
          true
        },
        showInsetWithRadios = memberNames.length == Constants.maxSchemeMembers,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "schemeMembersList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        ),
        yesHintText = yesHintText
      ),
      onSubmit = routes.SchemeMembersListController.onSubmit(srn, page, manualOrUpload)
    )
  }
}
