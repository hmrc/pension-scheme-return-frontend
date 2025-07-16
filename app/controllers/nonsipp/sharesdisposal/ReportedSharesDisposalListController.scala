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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import controllers.nonsipp.sharesdisposal.ReportedSharesDisposalListController._
import viewmodels.implicits._
import utils.ListUtils.ListOps
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import utils.IntUtils.toInt
import cats.implicits._
import _root_.config.Constants
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal._
import _root_.config.Constants.{maxDisposalsPerShare, maxSharesTransactions}
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.shares._
import play.api.mvc._
import _root_.config.RefinedTypes.{Max50, Max5000}
import com.google.inject.Inject
import views.html.ListView
import models.TypeOfShares._
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import models.HowSharesDisposed._
import utils.DateTimeUtils.localDateTimeShow
import models._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class ReportedSharesDisposalListController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = ReportedSharesDisposalListController.form(formProvider)

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
      viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
        getCompletedOrUpdatedTaskListStatus(
          request.userAnswers,
          request.previousUserAnswers.get,
          pages.nonsipp.sharesdisposal.Paths.disposedSharesTransaction
        ) == Updated
      } else {
        false
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
  )(implicit
    request: DataRequest[AnyContent]
  ): Result =
    getCompletedDisposals().map { completedDisposals =>
      val numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfSharesItems = request.userAnswers.map(SharesCompleted.all()).size
      val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems

      getSharesDisposalsWithIndexes(srn, completedDisposals).map { sharesDisposalsWithIndexes =>
        val allSharesFullyDisposed: Boolean = sharesDisposalsWithIndexes.forall {
          case ((shareIndex, disposalIndexes), _) =>
            disposalIndexes.exists { disposalIndex =>
              request.userAnswers.get(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex)).contains(0)
            }
        }

        val maximumDisposalsReached = numberOfDisposals >= maxSharesTransactions * maxDisposalsPerShare ||
          numberOfDisposals >= maxPossibleNumberOfDisposals ||
          allSharesFullyDisposed

        if (viewOnlyViewModel.nonEmpty || completedDisposals.values.exists(_.nonEmpty)) {
          Ok(
            view(
              form,
              viewModel(
                srn,
                page,
                mode,
                sharesDisposalsWithIndexes,
                numberOfDisposals,
                maxPossibleNumberOfDisposals,
                request.userAnswers,
                request.schemeDetails.schemeName,
                viewOnlyViewModel,
                showBackLink = showBackLink,
                maximumDisposalsReached = maximumDisposalsReached,
                allSharesFullyDisposed = allSharesFullyDisposed
              )
            )
          )
        } else {
          Redirect(routes.SharesDisposalController.onPageLoad(srn, NormalMode))
        }
      }.merge
    }.merge

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getCompletedDisposals()
        .traverse { completedDisposals =>
          val numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfSharesItems = request.userAnswers.map(SharesCompleted.all()).size
          val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems

          val allSharesFullyDisposed: Boolean = completedDisposals.forall { case (shareIndex, disposalIndexes) =>
            disposalIndexes.exists { disposalIndex =>
              request.userAnswers.get(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex)).contains(0)
            }
          }

          val maximumDisposalsReached = numberOfDisposals >= maxSharesTransactions * maxDisposalsPerShare ||
            numberOfDisposals >= maxPossibleNumberOfDisposals ||
            allSharesFullyDisposed

          if (maximumDisposalsReached) {
            Redirect(
              navigator.nextPage(ReportedSharesDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors =>
                  getSharesDisposalsWithIndexes(srn, completedDisposals)
                    .map { sharesDisposalsWithIndexes =>
                      BadRequest(
                        view(
                          errors,
                          viewModel(
                            srn,
                            page,
                            mode,
                            sharesDisposalsWithIndexes,
                            numberOfDisposals,
                            maxPossibleNumberOfDisposals,
                            request.userAnswers,
                            request.schemeDetails.schemeName,
                            viewOnlyViewModel = None,
                            showBackLink = true,
                            maximumDisposalsReached = maximumDisposalsReached,
                            allSharesFullyDisposed = allSharesFullyDisposed
                          )
                        )
                      )
                    }
                    .merge
                    .pure[Future],
                reportAnotherDisposal =>
                  if (reportAnotherDisposal) {
                    Redirect(
                      navigator.nextPage(
                        ReportedSharesDisposalListPage(srn, reportAnotherDisposal),
                        mode,
                        request.userAnswers
                      )
                    ).pure[Future]
                  } else {
                    for {
                      updatedUserAnswers <- request.userAnswers
                        .set(SharesDisposalCompleted(srn), SectionCompleted)
                        .mapK[Future]
                      _ <- saveService.save(updatedUserAnswers)
                    } yield Redirect(
                      navigator.nextPage(
                        ReportedSharesDisposalListPage(srn, reportAnotherDisposal),
                        mode,
                        updatedUserAnswers
                      )
                    )
                  }
              )
          }
        }
        .map(_.merge)
  }

  private def getSharesDisposalsWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(implicit
    request: DataRequest[?]
  ): Either[Result, List[((Max5000, List[Max50]), SectionCompleted)]] =
    disposals
      .map { case indexes @ (index, _) =>
        index -> request.userAnswers
          .get(SharesCompleted(srn, index))
          .toRight(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          .map(sharesDisposal => (indexes, sharesDisposal))
      }
      .toList
      .sortBy { case (index, _) => index.value }
      .map { case (_, result) => result }
      .sequence

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
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) { implicit request =>
      val showBackLink = false
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = if (request.previousUserAnswers.nonEmpty) {
          getCompletedOrUpdatedTaskListStatus(
            request.userAnswers,
            request.previousUserAnswers.get,
            pages.nonsipp.sharesdisposal.Paths.disposedSharesTransaction
          ) == Updated
        } else {
          false
        },
        year = year,
        currentVersion = (current - 1).max(0),
        previousVersion = (previous - 1).max(0),
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )
      onPageLoadCommon(srn, page, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
    }

  private def getCompletedDisposals()(implicit request: DataRequest[?]): Either[Result, Map[Max5000, List[Max50]]] = {
    val all = request.userAnswers
      .map(SharesCompleted.all())
    Right(
      all.keys.toList
        .refine[Max5000.Refined]
        .map { index =>
          index -> request.userAnswers
            .map(SharesDisposalProgress.all(index))
            .filter(_._2.completed)
            .keys
            .toList
            .refine[Max50.Refined]
        }
        .toMap
    )
  }
}

object ReportedSharesDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "sharesDisposal.reportedSharesDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers,
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    if (disposals.isEmpty && mode.isViewOnlyMode) {
      List(
        ListRow.viewNoLink(
          Message("sharesDisposal.reportedSharesDisposalList.view.none", schemeName),
          "sharesDisposal.reportedSharesDisposalList.view.none.value"
        )
      )
    } else {
      disposals.flatMap { case (shareIndex, disposalIndexes) =>
        disposalIndexes.sortBy(_.value).map { disposalIndex =>
          val sharesDisposalData = SharesDisposalData(
            shareIndex,
            disposalIndex,
            userAnswers.get(TypeOfSharesHeldPage(srn, shareIndex)).get,
            userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).get,
            userAnswers.get(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex)).get
          )

          (mode, viewOnlyViewModel) match {
            case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _))) =>
              ListRow.view(
                buildMessage("sharesDisposal.reportedSharesDisposalList.row", sharesDisposalData),
                routes.SharesDisposalCYAController
                  .onPageLoadViewOnly(srn, shareIndex, disposalIndex, year, currentVersion, previousVersion)
                  .url,
                buildMessage(
                  "sharesDisposal.reportedSharesDisposalList.row.view.hidden",
                  sharesDisposalData
                )
              )
            case (_, _) =>
              ListRow(
                buildMessage("sharesDisposal.reportedSharesDisposalList.row", sharesDisposalData),
                changeUrl = routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
                  .url,
                changeHiddenText = buildMessage(
                  "sharesDisposal.reportedSharesDisposalList.row.change.hidden",
                  sharesDisposalData
                ),
                removeUrl =
                  routes.RemoveShareDisposalController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode).url,
                removeHiddenText = buildMessage(
                  "sharesDisposal.reportedSharesDisposalList.row.remove.hidden",
                  sharesDisposalData
                )
              )
          }
        }
      }.toList
    }

  private def buildMessage(messageString: String, sharesDisposalData: SharesDisposalData): Message =
    sharesDisposalData match {
      case SharesDisposalData(_, _, typeOfShares, companyName, typeOfDisposal) =>
        val sharesType = typeOfShares match {
          case SponsoringEmployer => "sharesDisposal.reportedSharesDisposalList.typeOfShares.sponsoringEmployer"
          case Unquoted => "sharesDisposal.reportedSharesDisposalList.typeOfShares.unquoted"
          case ConnectedParty => "sharesDisposal.reportedSharesDisposalList.typeOfShares.connectedParty"
        }
        val disposalType = typeOfDisposal match {
          case Sold => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.sold"
          case Redeemed => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.redeemed"
          case Transferred => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.transferred"
          case Other(_) => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.other"
        }
        Message(messageString, sharesType, companyName, disposalType)
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    sharesDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int,
    userAnswers: UserAnswers,
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    maximumDisposalsReached: Boolean,
    allSharesFullyDisposed: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val disposals: Map[Max5000, List[Max50]] =
      sharesDisposalsWithIndexes.map { case ((shareIndex, disposalIndexes), _) => (shareIndex, disposalIndexes) }.toMap

    val (title, heading) = ((mode, numberOfDisposals) match {
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals == 0 =>
        (
          "sharesDisposal.reportedSharesDisposalList.view.title",
          "sharesDisposal.reportedSharesDisposalList.view.heading.none"
        )
      case (ViewOnlyMode, _) if numberOfDisposals > 1 =>
        (
          "sharesDisposal.reportedSharesDisposalList.view.title.plural",
          "sharesDisposal.reportedSharesDisposalList.view.heading.plural"
        )
      case _ =>
        (
          "sharesDisposal.reportedSharesDisposalList.title",
          "sharesDisposal.reportedSharesDisposalList.heading"
        )
    }) match {
      case (title, heading) =>
        (Message(title, numberOfDisposals), Message(heading, numberOfDisposals))
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.reportedSharesDisposalListSize,
      totalSize = numberOfDisposals,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.ReportedSharesDisposalListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          routes.ReportedSharesDisposalListController.onPageLoad(srn, _)
      }
    )

    val conditionalInsetText: DisplayMessage =
      if (numberOfDisposals >= maxSharesTransactions * maxDisposalsPerShare) {
        Message("sharesDisposal.reportedSharesDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals || allSharesFullyDisposed) {
        ParagraphMessage("sharesDisposal.reportedSharesDisposalList.inset.allSharesDisposed.paragraph1") ++
          ParagraphMessage("sharesDisposal.reportedSharesDisposalList.inset.allSharesDisposed.paragraph2")
      } else {
        Message("")
      }

    val showRadios = !maximumDisposalsReached && !mode.isViewOnlyMode &&
      numberOfDisposals < maxPossibleNumberOfDisposals && !allSharesFullyDisposed

    val description = Option.when(
      !maximumDisposalsReached && !allSharesFullyDisposed
    )(
      ParagraphMessage("sharesDisposal.reportedSharesDisposalList.description")
    )

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = description,
      page = ListViewModel(
        inset = conditionalInsetText,
        sections = List(ListSection(rows(srn, mode, disposals, userAnswers, viewOnlyViewModel, schemeName))),
        Message("sharesDisposal.reportedSharesDisposalList.radios"),
        showRadios = showRadios,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "sharesDisposal.reportedSharesDisposalList.pagination.label",
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
      onSubmit = routes.ReportedSharesDisposalListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "sharesDisposal.reportedSharesDisposalList.view.link",
                routes.ReportedSharesDisposalListController
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
          onSubmit = viewOnlyViewModel match {
            case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
              routes.ReportedSharesDisposalListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              routes.ReportedSharesDisposalListController
                .onSubmit(srn, page, mode)
          }
        )
      },
      showBackLink = showBackLink
    )
  }

  case class SharesDisposalData(
    shareIndex: Max5000,
    disposalIndex: Max50,
    sharesType: TypeOfShares,
    companyName: String,
    disposalMethod: HowSharesDisposed
  )

  case class SharesDisposalDataRemoval(
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    disposalMethod: HowSharesDisposed
  )
}
