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

package controllers.nonsipp.memberdetails

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.SchemeMembersListPage
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.Max300
import controllers.PSRController
import config.Constants
import cats.implicits.catsSyntaxApplicativeId
import config.Constants.maxSchemeMembers
import forms.YesNoPageFormProvider
import models.{ManualOrUpload, Mode, Pagination}
import models.CheckOrChange.Change
import views.html.ListView
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import play.api.Logger
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberStatusImplicits.MembersStatusOps
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import play.api.i18n.MessagesApi
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import utils.MapUtils.UserAnswersMapOps
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

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

        val mapOfFullName = membersDetails.view.mapValues(_.fullName).toMap
        filterDeletedMembers(mapOfFullName, srn).map { filteredMembers =>
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
      val lengthOfMembersDetails = membersDetails.size
      val mapOfFullName = membersDetails.view.mapValues(_.fullName).toMap

      form(manualOrUpload, lengthOfMembersDetails >= maxSchemeMembers)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            filterDeletedMembers(mapOfFullName, srn).map { filteredMembers =>
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
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    request.userAnswers,
                    optFallbackCall = Some(
                      controllers.nonsipp.memberdetails.routes.SchemeMembersListController
                        .onPageLoad(srn, page, manualOrUpload)
                    )
                  )
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
    nameMap: Map[String, String],
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, List[(Max300, (String, String), MemberState)]] = {

    val maybeMembersDetails =
      nameMap.toList.zipWithIndexToMap
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
          val zippedMembers = membersDetails.zip(memberStates).toMap
          val memberWithStates = zippedMembers.collect {
            case ((i1, details), (i2, state)) if i1.value == i2.value => Some((i1, details, state))
            case ((i1, details), (i2, state)) if i1.value != i2.value => Some((i1, details, state))
            case _ => None
          }.flatten

          if (memberWithStates.size != memberStates.size) {
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
    filteredMembers: List[(Max300, (String, String), MemberState)]
  ): FormPageViewModel[ListViewModel] = {

    val lengthOfFilteredMembers = filteredMembers.length

    val rows: List[ListRow] = filteredMembers
      .sortBy { case (_, (_, name), _) => name }
      .map {
        case (_, (unrefinedIndex, memberFullName), _) =>
          val index = refineV[Max300.Refined](unrefinedIndex.toInt + 1).toOption.get
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
      title = Message(title, lengthOfFilteredMembers),
      heading = Message(heading, lengthOfFilteredMembers),
      description = Option
        .when(lengthOfFilteredMembers < Constants.maxSchemeMembers)(ParagraphMessage("schemeMembersList.paragraph")),
      page = ListViewModel(
        inset = "schemeMembersList.inset",
        rows,
        radioText,
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
      refresh = None,
      Message("site.saveAndContinue"),
      None,
      onSubmit = routes.SchemeMembersListController.onSubmit(srn, page, manualOrUpload)
    )
  }
}
