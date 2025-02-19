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

import services.SaveService
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils._
import cats.implicits.{toShow, toTraverseOps, _}
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import controllers.nonsipp.otherassetsheld.OtherAssetsListController._
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.otherassetsheld._
import com.google.inject.Inject
import config.RefinedTypes.Max5000
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import utils.nonsipp.check.OtherAssetsCheckStatusUtils
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import utils.MapUtils.UserAnswersMapOps
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
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private implicit val logger: Logger = Logger(getClass)

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

    val filledForm = request.userAnswers.get(OtherAssetsListPage(srn)).fold(form)(form.fill)

    if (indexes.nonEmpty || mode.isViewOnlyMode) {
      otherAssetsToTraverse(srn).map {
        case (otherAssetsToCheck, otherAssets) =>
          Ok(
            view(
              filledForm,
              viewModel(
                srn,
                page,
                mode,
                otherAssets,
                otherAssetsToCheck,
                request.schemeDetails.schemeName,
                viewOnlyViewModel,
                showBackLink,
                isPrePopulation
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
      otherAssetsToTraverse(srn).traverse {
        case (otherAssetsToCheck, otherAssets) =>
          if (otherAssetsToCheck.size + otherAssets.size >= Constants.maxOtherAssetsTransactions) {
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
                  Future.successful(
                    BadRequest(
                      view(
                        errors,
                        viewModel(
                          srn = srn,
                          page = page,
                          mode = mode,
                          otherAssets = otherAssets,
                          otherAssetsToCheck = otherAssetsToCheck,
                          schemeName = request.schemeDetails.schemeName,
                          viewOnlyViewModel = None,
                          showBackLink = true,
                          isPrePop = isPrePopulation
                        )
                      )
                    )
                  )
                },
                addAnother =>
                  for {
                    updatedUserAnswers <- Future.fromTry(request.userAnswers.set(OtherAssetsListPage(srn), addAnother))
                    _ <- saveService.save(updatedUserAnswers)
                  } yield Redirect(
                    navigator.nextPage(OtherAssetsListPage(srn), mode, updatedUserAnswers)
                  )
              )
          }
      }.merge
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

  private def otherAssetsToTraverse(srn: Srn)(
    implicit request: DataRequest[_],
    logger: Logger
  ): Either[Result, (List[OtherAssetsData], List[OtherAssetsData])] = {
    // if return has been pre-populated, partition shares by those that need to be checked
    def buildOtherAssets(index: Max5000): Either[Result, OtherAssetsData] =
      for {
        nameOfOtherAsset <- requiredPage(WhatIsOtherAssetPage(srn, index))
      } yield OtherAssetsData(index, nameOfOtherAsset)

    if (isPrePopulation) {
      for {
        indexes <- request.userAnswers
          .map(WhatIsOtherAssetPages(srn))
          .refine[Max5000.Refined]
          .map(_.keys.toList)
          .getOrRecoverJourney
        otherAssets <- indexes.traverse(buildOtherAssets)
      } yield otherAssets.partition(
        otherAssets => OtherAssetsCheckStatusUtils.checkOtherAssetsRecord(request.userAnswers, srn, otherAssets.index)
      )
    } else {
      val noOtherAssetsToCheck = List.empty[OtherAssetsData]
      for {
        indexes <- request.userAnswers
          .map(WhatIsOtherAssetPages(srn))
          .refine[Max5000.Refined]
          .map(_.keys.toList)
          .getOrRecoverJourney
        otherAssets <- indexes.traverse(buildOtherAssets)
      } yield (noOtherAssetsToCheck, otherAssets)
    }
  }
}

object OtherAssetsListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "otherAssets.list.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    otherAssetsList: List[OtherAssetsData],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String,
    check: Boolean = false
  ): List[ListRow] =
    (otherAssetsList, mode) match {
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
            val otherAssetsMessage = Message("otherAssets.list.row", nameOfOtherAssets)
            (mode, viewOnlyViewModel) match {
              case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
                ListRow.view(
                  text = otherAssetsMessage,
                  url = routes.OtherAssetsCYAController.onPageLoadViewOnly(srn, index, year, current, previous).url,
                  hiddenText = Message("otherAssets.list.row.view.hiddenText", otherAssetsMessage)
                )
              case _ if check =>
                ListRow.check(
                  text = otherAssetsMessage,
                  url = routes.OtherAssetsCheckAndUpdateController.onPageLoad(srn, index).url,
                  hiddenText = Message("site.check.param", nameOfOtherAssets)
                )
              case _ =>
                ListRow(
                  text = otherAssetsMessage,
                  changeUrl = routes.OtherAssetsCYAController.onPageLoad(srn, index, CheckMode).url,
                  changeHiddenText = Message("otherAssets.list.row.change.hiddenText", otherAssetsMessage),
                  removeUrl = routes.RemoveOtherAssetController.onPageLoad(srn, index, NormalMode).url,
                  removeHiddenText = Message("otherAssets.list.row.remove.hiddenText", otherAssetsMessage)
                )
            }
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    otherAssets: List[OtherAssetsData],
    otherAssetsToCheck: List[OtherAssetsData],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    isPrePop: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val otherAssetsSize = if (isPrePop) otherAssets.length + otherAssetsToCheck.length else otherAssets.length

    val (title, heading) = (mode match {
      // View Only
      case ViewOnlyMode if otherAssetsSize == 0 =>
        ("otherAssets.list.view.title.none", "otherAssets.list.view.heading.none")
      case ViewOnlyMode if otherAssetsSize > 1 =>
        ("otherAssets.list.view.title.plural", "otherAssets.list.view.heading.plural")
      case ViewOnlyMode =>
        ("otherAssets.list.view.title", "otherAssets.list.view.heading")
      // PrePop
      case _ if isPrePop && otherAssets.nonEmpty =>
        ("otherAssets.list.title.prepop.check", "otherAssets.list.heading.prepop.check")
      case _ if isPrePop && otherAssetsSize > 1 =>
        ("otherAssets.list.title.prepop.plural", "otherAssets.list.heading.prepop.plural")
      case _ if isPrePop =>
        ("otherAssets.list.title.prepop", "otherAssets.list.heading.prepop")
      // Normal
      case _ if otherAssetsSize > 1 =>
        ("otherAssets.list.title.plural", "otherAssets.list.heading.plural")
      case _ =>
        ("otherAssets.list.title", "otherAssets.list.heading")
    }) match {
      case (title, heading) =>
        (Message(title, otherAssetsSize), Message(heading, otherAssetsSize))
    }

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.otherAssetsPageSize >= otherAssetsSize) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.otherAssetsPageSize,
      totalSize = otherAssetsSize,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.OtherAssetsListController.onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          routes.OtherAssetsListController.onPageLoad(srn, _, NormalMode)
      }
    )

    val paragraph = Option.when(otherAssetsSize < Constants.otherAssetsPageSize) {
      if (otherAssetsToCheck.nonEmpty) {
        ParagraphMessage("otherAssets.list.description.prepop", "otherAssets.list.description") ++
          ParagraphMessage("otherAssets.list.description.disposal")
      } else {
        ParagraphMessage("otherAssets.list.description") ++
          ParagraphMessage("otherAssets.list.description.disposal")
      }
    }

    val conditionalInsetText: DisplayMessage = {
      if (otherAssetsSize >= Constants.maxOtherAssetsTransactions) {
        ParagraphMessage("otherAssets.list.inset")
      } else {
        Message("")
      }
    }

    val sections = {
      if (isPrePop) {
        Option
          .when(otherAssetsToCheck.nonEmpty)(
            ListSection(
              heading = Some("otherAssets.list.section.check"),
              rows = rows(srn, mode, otherAssetsToCheck, viewOnlyViewModel, schemeName, check = true)
            )
          )
          .toList ++
          Option
            .when(otherAssets.nonEmpty)(
              ListSection(
                heading = Some("otherAssets.list.section.added"),
                rows = rows(srn, mode, otherAssets, viewOnlyViewModel, schemeName)
              )
            )
            .toList
      } else {
        List(
          ListSection(rows = rows(srn, mode, otherAssets, viewOnlyViewModel, schemeName))
        )
      }
    }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = paragraph,
      page = ListViewModel(
        inset = conditionalInsetText,
        sections = sections,
        radioText = Message("otherAssets.list.radios"),
        showRadios = otherAssetsSize < Constants.maxOtherAssetsTransactions,
        showInsetWithRadios = !(otherAssetsSize < Constants.maxOtherAssetsTransactions),
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
                content = "otherAssets.list.view.link",
                url = routes.OtherAssetsListController
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
          onSubmit = routes.OtherAssetsListController
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
