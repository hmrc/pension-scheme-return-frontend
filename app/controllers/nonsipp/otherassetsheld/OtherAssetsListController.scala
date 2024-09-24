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
      onPageLoadCommon(srn, page, mode, showBackLink = true)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
    val showBackLink = true
    val viewOnlyViewModel = ViewOnlyViewModel(
      viewOnlyUpdated = request.previousUserAnswers match {
        case Some(previousUserAnswers) =>
          getCompletedOrUpdatedTaskListStatus(
            request.userAnswers,
            previousUserAnswers,
            pages.nonsipp.otherassetsheld.Paths.otherAssetsTransactions
          ) == Updated
        case _ => false
      },
      year = year,
      currentVersion = current,
      previousVersion = previous,
      compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
    )
    onPageLoadCommon(srn, page, mode, Some(viewOnlyViewModel), showBackLink)
  }

  def onPageLoadCommon(
    srn: Srn,
    page: Int,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  )(
    implicit request: DataRequest[AnyContent]
  ): Result = {
    val indexes: List[Max5000] =
      request.userAnswers.map(OtherAssetsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty || mode.isViewOnlyMode) {
      otherAssetsData(srn, indexes).map { data =>
        val filledForm =
          request.userAnswers.get(OtherAssetsListPage(srn)).fold(form)(form.fill)
        Ok(
          view(
            filledForm,
            OtherAssetsListController.viewModel(
              srn,
              page,
              mode,
              data,
              request.schemeDetails.schemeName,
              viewOnlyViewModel,
              showBackLink = showBackLink
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
                  BadRequest(
                    view(
                      errors,
                      viewModel(srn, page, mode, data, request.schemeDetails.schemeName, showBackLink = true)
                    )
                  )
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

  def onPreviousViewOnly(
    srn: Srn,
    page: Int,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) {
    implicit request =>
      val showBackLink = false
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              pages.nonsipp.otherassetsheld.Paths.otherAssetsTransactions
            ) == Updated
          case _ => false
        },
        year = year,
        currentVersion = (current - 1).max(0),
        previousVersion = (previous - 1).max(0),
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )
      onPageLoadCommon(srn, page, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
  }

  private def otherAssetsData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[OtherAssetsListController.OtherAssetsData]] =
    indexes
      .sortBy(x => x.value)
      .map { index =>
        for {
          nameOfOtherAssets <- requiredPage(WhatIsOtherAssetPage(srn, index))
        } yield OtherAssetsListController.OtherAssetsData(index, nameOfOtherAssets)
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
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    (memberList, mode) match {
      case (Nil, mode) if mode.isViewOnlyMode =>
        List(
          ListRow.viewNoLink(
            Message("otherAssets.list.view.none", schemeName),
            "otherAssets.list.view.none.value"
          )
        )
      case (Nil, mode) if !mode.isViewOnlyMode =>
        List()
      case (list, _) =>
        list.map {
          case OtherAssetsData(index, nameOfOtherAssets) =>
            val otherAssetsMessage =
              Message("otherAssets.list.row", nameOfOtherAssets.show)

            (mode, viewOnlyViewModel) match {
              case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
                ListRow.view(
                  otherAssetsMessage,
                  controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                    .onPageLoadViewOnly(srn, index, year, current, previous)
                    .url,
                  Message("otherAssets.list.row.view.hiddenText", otherAssetsMessage)
                )
              case _ =>
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
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[OtherAssetsData],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  ): FormPageViewModel[ListViewModel] = {
    val lengthOfData = data.length

    val (title, heading) = ((mode, lengthOfData) match {
      case (ViewOnlyMode, numberOfLoans) if numberOfLoans == 0 =>
        ("otherAssets.list.view.title", "otherAssets.list.view.heading.none")
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
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
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
        rows = rows(srn, mode, data, viewOnlyViewModel, schemeName),
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
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "otherAssets.list.view.link",
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
                  .onPreviousViewOnly(
                    srn,
                    page,
                    viewOnly.year,
                    viewOnly.currentVersion,
                    viewOnly.previousVersion
                  )
                  .url
              )
            )
          } else {
            None
          },
          submittedText = viewOnly.compilationOrSubmissionDate
            .fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
          title = title,
          heading = heading,
          buttonText = "site.return.to.tasklist",
          onSubmit = controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }

  case class OtherAssetsData(
    index: Max5000,
    nameOfOtherAssets: String
  )

}
