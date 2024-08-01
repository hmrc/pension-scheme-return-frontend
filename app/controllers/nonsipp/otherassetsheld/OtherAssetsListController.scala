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

package controllers.nonsipp.otherassetsheld

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils._
import config.Refined.Max5000
import controllers.PSRController
import cats.implicits.{catsSyntaxApplicativeId, toShow, toTraverseOps}
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import controllers.nonsipp.otherassetsheld.OtherAssetsListController._
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.otherassetsheld._
import com.google.inject.Inject
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.Named

class OtherAssetsListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = OtherAssetsListController.form(formProvider)

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
    val indexes: List[Max5000] =
      request.userAnswers.map(OtherAssetsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty) {
      otherAssetsData(srn, indexes).map { data =>
        val filledForm =
          request.userAnswers.get(OtherAssetsListPage(srn)).fold(form)(form.fill)
        Ok(
          view(
            filledForm,
            viewModel(
              srn,
              page,
              mode,
              data,
              viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getCompletedOrUpdatedTaskListStatus(
                  request.userAnswers,
                  request.previousUserAnswers.get,
                  pages.nonsipp.otherassetsheld.Paths.otherAssetsTransactions
                ) == Updated
              } else {
                false
              },
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
            )
          )
        )
      }.merge
    } else {
      Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val indexes: List[Max5000] =
        request.userAnswers.map(OtherAssetsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.size >= Constants.maxOtherAssetsTransactions) {
        Future.successful(
          Redirect(
            navigator.nextPage(OtherAssetsListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors => {
              otherAssetsData(srn, indexes)
                .map { data =>
                  BadRequest(view(errors, viewModel(srn, page, mode, data, viewOnlyUpdated = false, None, None, None)))
                }
                .merge
                .pure[Future]
            },
            addAnother =>
              for {
                updatedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .set(
                        OtherAssetsJourneyStatus(srn),
                        if (!addAnother) SectionStatus.Completed else SectionStatus.InProgress
                      )
                      .set(OtherAssetsListPage(srn), addAnother)
                  )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (!addAnother) {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall =
                      controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, page, mode)
                  )
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(OtherAssetsListPage(srn), mode, updatedUserAnswers)
                  )
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
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }

  private def otherAssetsData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[OtherAssetsData]] =
    indexes
      .sortBy(x => x.value)
      .map { index =>
        for {
          nameOfOtherAssets <- requiredPage(WhatIsOtherAssetPage(srn, index))
        } yield OtherAssetsData(index, nameOfOtherAssets)
      }
      .sequence
}

object OtherAssetsListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "otherAssets.list.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[OtherAssetsData],
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[ListRow] =
    memberList.map {
      case OtherAssetsData(index, nameOfOtherAssets) =>
        val otherAssetsMessage =
          Message("otherAssets.list.row", nameOfOtherAssets.show)

        if (mode.isViewOnlyMode) {
          (mode, optYear, optCurrentVersion, optPreviousVersion) match {
            case (ViewOnlyMode, Some(year), Some(current), Some(previous)) =>
              ListRow.view(
                otherAssetsMessage,
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoadViewOnly(srn, index, year, current, previous)
                  .url,
                Message("otherAssets.list.row.change.hiddenText", otherAssetsMessage)
              )
          }
        } else {
          ListRow(
            otherAssetsMessage,
            changeUrl = controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
              .onPageLoad(srn, index, CheckMode)
              .url,
            changeHiddenText = Message("otherAssets.list.row.change.hiddenText", otherAssetsMessage),
            removeUrl = controllers.nonsipp.otherassetsheld.routes.RemoveOtherAssetController
              .onPageLoad(srn, index, NormalMode)
              .url,
            removeHiddenText = Message("otherAssets.list.row.remove.hiddenText", otherAssetsMessage)
          )
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[OtherAssetsData],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ListViewModel] = {
    val lengthOfData = data.length

    val (title, heading) = ((mode, lengthOfData) match {
      case (ViewOnlyMode, lengthOfData) if lengthOfData > 1 =>
        ("otherAssets.list.view.title.plural", "otherAssets.list.view.heading.plural")
      case (ViewOnlyMode, _) =>
        ("otherAssets.list.view.title", "otherAssets.list.view.heading")
      case (_, lengthOfData) if lengthOfData > 1 =>
        ("otherAssets.list.title.plural", "otherAssets.list.heading.plural")
      case _ =>
        ("otherAssets.list.title", "otherAssets.list.heading")
    }) match {
      case (title, heading) =>
        (Message(title, lengthOfData), Message(heading, lengthOfData))
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.pageSize,
      totalSize = data.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, _, mode)
      }
    )

    val conditionalInsetText: DisplayMessage = {
      if (data.size >= Constants.maxOtherAssetsTransactions) {
        ParagraphMessage("otherAssets.list.inset")
      } else {
        Message("")
      }
    }
    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = Some(ParagraphMessage("otherAssets.list.description")),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows = rows(srn, mode, data, optYear, optCurrentVersion, optPreviousVersion),
        radioText = Message("otherAssets.list.radios"),
        showRadios = data.size < Constants.maxOtherAssetsTransactions,
        showInsetWithRadios = !(data.length < Constants.maxOtherAssetsTransactions),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "otherAssets.list.pagination.label",
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
      onSubmit = controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = Option.when(mode.isViewOnlyMode) {
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if (optYear.nonEmpty && currentVersion > 1 && previousVersion > 0) =>
              Some(
                LinkMessage(
                  "otherAssets.list.view.link",
                  controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
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
          title = title,
          heading = heading,
          buttonText = "site.return.to.tasklist",
          onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion)) =>
              controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
                .onSubmit(srn, page, mode)
          }
        )
      }
    )
  }

  case class OtherAssetsData(
    index: Max5000,
    nameOfOtherAssets: String
  )

}
