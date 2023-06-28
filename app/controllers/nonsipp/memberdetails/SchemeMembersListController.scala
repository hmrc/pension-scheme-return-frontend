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
import config.Refined.OneTo300
import controllers.actions._
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import eu.timepit.refined._
import forms.YesNoPageFormProvider
import models.CheckOrChange.Change
import models.SchemeId.Srn
import models.{ManualOrUpload, Mode, Pagination}
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberdetails.SchemeMembersListPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel, PaginatedViewModel}
import views.html.ListView

import javax.inject.Named

class SchemeMembersListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends FrontendBaseController
    with I18nSupport {

  private def form(manualOrUpload: ManualOrUpload): Form[Boolean] =
    SchemeMembersListController.form(formProvider, manualOrUpload)

  def onPageLoad(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val membersDetails = request.userAnswers.membersDetails(srn)
      if (membersDetails.isEmpty) {
        Redirect(routes.PensionSchemeMembersController.onPageLoad(srn))
      } else {
        Ok(view(form(manualOrUpload), viewModel(srn, page, manualOrUpload, mode, membersDetails.map(_.fullName))))
      }
    }

  def onSubmit(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val membersDetails = request.userAnswers.membersDetails(srn)
      if (membersDetails.length == maxSchemeMembers) {
        Redirect(
          navigator.nextPage(SchemeMembersListPage(srn, addMember = false, manualOrUpload), mode, request.userAnswers)
        )
      } else {
        form(manualOrUpload)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              BadRequest(
                view(formWithErrors, viewModel(srn, page, manualOrUpload, mode, membersDetails.map(_.fullName)))
              ),
            value =>
              Redirect(navigator.nextPage(SchemeMembersListPage(srn, value, manualOrUpload), mode, request.userAnswers))
          )
      }
    }
}

object SchemeMembersListController {
  def form(formProvider: YesNoPageFormProvider, manualOrUpload: ManualOrUpload): Form[Boolean] = formProvider(
    manualOrUpload.fold(manual = "schemeMembersList.error.required", upload = "membersUploaded.error.required")
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

    FormPageViewModel(
      Message(titleKey, memberNames.length),
      Message(headingKey, memberNames.length),
      ListViewModel(
        inset = "schemeMembersList.inset",
        rows,
        manualOrUpload.fold(manual = "schemeMembersList.radio", upload = "membersUploaded.radio"),
        showRadios = memberNames.length < Constants.maxSchemeMembers,
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
        yesHintText = manualOrUpload.fold(manual = None, upload = Some("membersUploaded.radio.yes.hint"))
      ),
      onSubmit = routes.SchemeMembersListController.onSubmit(srn, page, manualOrUpload)
    )
  }
}
