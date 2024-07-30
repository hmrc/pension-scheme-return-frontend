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

package controllers.nonsipp.landorproperty

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.Max5000
import controllers.PSRController
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPages, LandOrPropertyListPage}
import cats.implicits.toShow
import config.Constants.maxLandOrProperties
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import utils.nonsipp.TaskListStatusUtils.{getCompletedOrUpdatedTaskListStatus, getLandOrPropertyTaskListStatusAndLink}
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.Future

import java.time.LocalDateTime
import javax.inject.Named

class LandOrPropertyListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form: Form[Boolean] = LandOrPropertyListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, page, mode)(implicitly)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
    onPageLoadCommon(srn, page, mode)(implicitly)
  }

  def onPageLoadCommon(srn: Srn, page: Int, mode: Mode)(implicit request: DataRequest[AnyContent]): Result = {
    val userAnswers = request.userAnswers
    val (status, incompleteLandOrPropertyUrl) = getLandOrPropertyTaskListStatusAndLink(userAnswers, srn)
    if (status == TaskListStatus.Completed) {
      val addresses = userAnswers.map(LandOrPropertyAddressLookupPages(srn))
      val viewModel =
        LandOrPropertyListController.viewModel(
          srn,
          page,
          mode,
          addresses,
          viewOnlyUpdated = if (mode.isViewOnlyMode && request.previousUserAnswers.nonEmpty) {
            getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              request.previousUserAnswers.get,
              pages.nonsipp.landorproperty.Paths.landOrProperty
            ) == Updated
          } else {
            false
          },
          optYear = request.year,
          optCurrentVersion = request.currentVersion,
          optPreviousVersion = request.previousVersion,
          compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
        )
      Ok(view(form, viewModel))
    } else if (status == TaskListStatus.InProgress) {
      Redirect(incompleteLandOrPropertyUrl)
    } else {
      Redirect(routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode))
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val addresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn))

    if (addresses.size == maxLandOrProperties) {
      Redirect(navigator.nextPage(LandOrPropertyListPage(srn, addLandOrProperty = false), mode, request.userAnswers))
    } else {
      val viewModel =
        LandOrPropertyListController.viewModel(srn, page, mode, addresses, viewOnlyUpdated = false, None, None, None)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer =>
            Redirect(
              navigator.nextPage(LandOrPropertyListPage(srn, addLandOrProperty = answer), mode, request.userAnswers)
            )
        )
    }
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }

  def onPreviousViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          routes.LandOrPropertyListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }
}

object LandOrPropertyListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "landOrPropertyList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    addresses: Map[String, Address],
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[ListRow] =
    addresses
      .flatMap {
        case (index, address) =>
          refineV[Max5000.Refined](index.toInt + 1).fold(
            _ => Nil,
            index =>
              if (mode.isViewOnlyMode) {
                (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                  case (ViewOnlyMode, Some(year), Some(current), Some(previous)) =>
                    List(
                      index -> ListRow.view(
                        address.addressLine1,
                        routes.LandOrPropertyCYAController.onPageLoadViewOnly(srn, index, year, current, previous).url,
                        Message("landOrPropertyList.row.change.hiddenText", address.addressLine1)
                      )
                    )
                  case _ => Nil
                }
              } else {
                List(
                  index -> ListRow(
                    address.addressLine1,
                    changeUrl = routes.LandOrPropertyCYAController.onPageLoad(srn, index, CheckMode).url,
                    changeHiddenText = Message("landOrPropertyList.row.change.hiddenText", address.addressLine1),
                    removeUrl = routes.RemovePropertyController.onPageLoad(srn, index, mode).url,
                    removeHiddenText = Message("landOrPropertyList.row.remove.hiddenText", address.addressLine1)
                  )
                )
              }
          )
      }
      .toList
      .sortBy { case (index, _) => index.value }
      .map { case (_, listRow) => listRow }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    addresses: Map[String, Address],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ListViewModel] = {

    val title = if (addresses.size == 1) "landOrPropertyList.title" else "landOrPropertyList.title.plural"
    val heading = if (addresses.size == 1) "landOrPropertyList.heading" else "landOrPropertyList.heading.plural"
    val paragraph =
      if (addresses.size < maxLandOrProperties) Some(ParagraphMessage("landOrPropertyList.paragraph")) else None

    val currentPage = if ((page - 1) * Constants.landOrPropertiesSize >= addresses.size) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.landOrPropertiesSize,
      addresses.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          routes.LandOrPropertyListController.onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.LandOrPropertyListController.onPageLoad(srn, _, NormalMode)
      }
    )

    FormPageViewModel(
      mode = mode,
      title = Message(title, addresses.size),
      heading = Message(heading, addresses.size),
      description = paragraph,
      page = ListViewModel(
        inset = "landOrPropertyList.inset",
        rows(srn, mode, addresses, optYear, optCurrentVersion, optPreviousVersion),
        Message("landOrPropertyList.radios"),
        showRadios = addresses.size < Constants.maxLandOrProperties,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "landOrPropertyList.pagination.label",
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
      onSubmit = routes.LandOrPropertyListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = if (mode.isViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if optYear.nonEmpty && currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "landOrPropertyList.viewOnly.link",
                    routes.LandOrPropertyListController
                      .onPreviousViewOnly(
                        srn,
                        page,
                        year,
                        currentVersion,
                        previousVersion
                      )
                      .url
                  )
                )
              case _ => None
            },
            submittedText =
              compilationOrSubmissionDate.fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
            title = "landOrPropertyList.viewOnly.title",
            heading = Message(
              if (addresses.size == 1) "landOrPropertyList.viewOnly.heading"
              else "landOrPropertyList.viewOnly.heading.plural",
              addresses.size
            ),
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                routes.LandOrPropertyListController.onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                routes.LandOrPropertyListController.onSubmit(srn, page, mode)
            }
          )
        )
      } else {
        None
      }
    )
  }
}
