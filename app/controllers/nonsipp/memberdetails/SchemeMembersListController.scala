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

import cats.implicits.catsSyntaxApplicativeId
import com.google.inject.Inject
import config.Constants
import config.Constants.maxSchemeMembers
import config.Refined.Max300
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import forms.YesNoPageFormProvider
import models.CheckOrChange.Change
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{ManualOrUpload, Mode, Pagination}
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberStatusImplicits.MembersStatusOps
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberdetails.SchemeMembersListPage
import play.api.Logger
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PsrSubmissionService
import utils.MapUtils.{MapOps, UserAnswersMapOps}
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.ListView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class SchemeMembersListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  psrSubmissionService: PsrSubmissionService,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val logger: Logger = Logger(classOf[SchemeMembersListController])

  private def form(manualOrUpload: ManualOrUpload, maxNumberReached: Boolean): Form[Boolean] =
    SchemeMembersListController.form(formProvider, manualOrUpload, maxNumberReached)

  def onPageLoad(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val membersDetails = request.userAnswers.membersDetails(srn)
      if (membersDetails.isEmpty) {
        logger.error(s"no members found")
        Redirect(routes.PensionSchemeMembersController.onPageLoad(srn))
      } else {

        val listOfFullName = membersDetails.map(_.fullName)
        filterDeletedMembers(listOfFullName, srn).map { filteredMembers =>
          Ok(
            view(
              form(manualOrUpload, filteredMembers.size >= maxSchemeMembers),
              viewModel(srn, page, manualOrUpload, mode, filteredMembers)
            )
          )
        }.merge
      }
    }

  def onSubmit(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val membersDetails = request.userAnswers.membersDetails(srn)
      val lengthOfMembersDetails = membersDetails.length
      val listOfFullName = membersDetails.map(_.fullName)

      form(manualOrUpload, lengthOfMembersDetails >= maxSchemeMembers)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            filterDeletedMembers(listOfFullName, srn).map { filteredMembers =>
              BadRequest(
                view(
                  formWithErrors,
                  viewModel(srn, page, manualOrUpload, mode, filteredMembers)
                )
              )
            }
          }.merge.pure[Future],
          value => {
            if (lengthOfMembersDetails == maxSchemeMembers && value) {
              Future.successful(Redirect(routes.HowToUploadController.onPageLoad(srn)))
            } else {
              for {
                _ <- if (!value) {
                  psrSubmissionService.submitPsrDetails(srn, request.userAnswers)
                } else {
                  Future.successful(Some(()))
                }
              } yield Redirect(
                navigator.nextPage(SchemeMembersListPage(srn, value, manualOrUpload), mode, request.userAnswers)
              )
            }
          }
        )
    }

  // merges member details with member states and filters out any soft deleted members
  private def filterDeletedMembers(
    nameList: List[String],
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, List[(Max300, String, MemberState)]] = {

    val maybeMembersDetails =
      nameList.zipWithIndexToMap
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
          val sortedMemberDetails = membersDetails.sort({ Ordering.by(_.value) })
          val sortedMemberStates = memberStates.sort { Ordering.by(_.value) }
          val zippedMembers: List[((Max300, String), (Max300, MemberState))] =
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
    filteredMembers: List[(Max300, String, MemberState)]
  ): FormPageViewModel[ListViewModel] = {

    val lengthOfFilteredMembers = filteredMembers.length

    val rows: List[ListRow] = filteredMembers
      .sortBy(_._2)
      .map {
        case (index, memberFullName, _) =>
          ListRow(
            memberFullName,
            changeUrl = routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Change).url,
            changeHiddenText = Message("schemeMembersList.change.hidden", memberFullName),
            removeUrl = routes.RemoveMemberDetailsController.onPageLoad(srn, index, mode).url,
            removeHiddenText = Message("schemeMembersList.remove.hidden", memberFullName)
          )
      }

    val (title, heading) = if (lengthOfFilteredMembers > 1) {
      ("schemeMembersList.title.plural", "schemeMembersList.heading.plural")
    } else {
      ("schemeMembersList.title", "schemeMembersList.heading")
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.schemeMembersPageSize,
      rows.size,
      routes.SchemeMembersListController.onPageLoad(srn, _, manualOrUpload)
    )

    val radioText = manualOrUpload.fold(
      upload = "membersUploaded.radio",
      manual =
        if (lengthOfFilteredMembers < Constants.maxSchemeMembers) "schemeMembersList.radio" else "membersUploaded.radio"
    )
    val yesHintText = manualOrUpload.fold(
      upload = Some(Message("membersUploaded.radio.yes.hint")),
      manual = if (lengthOfFilteredMembers < Constants.maxSchemeMembers) {
        None
      } else {
        Some(Message("membersUploaded.radio.yes.hint"))
      }
    )

    FormPageViewModel(
      Message(title, lengthOfFilteredMembers),
      Message(heading, lengthOfFilteredMembers),
      ListViewModel(
        inset = "schemeMembersList.inset",
        rows,
        radioText,
        showRadios = lengthOfFilteredMembers < Constants.maxSchemeMembers,
        showInsetWithRadios = lengthOfFilteredMembers == Constants.maxSchemeMembers,
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
