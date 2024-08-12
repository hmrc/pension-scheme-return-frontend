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

import services.{PsrSubmissionService, SaveService}
import controllers.nonsipp.sharesdisposal.ReportedSharesDisposalListController._
import viewmodels.implicits._
import com.google.inject.Inject
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import cats.implicits._
import config.Constants.{maxDisposalsPerShare, maxSharesTransactions}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal._
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesCompleted, TypeOfSharesHeldPage}
import play.api.mvc._
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.ListView
import models.TypeOfShares._
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import models.HowSharesDisposed._
import play.api.Logging
import navigation.Navigator
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

class ReportedSharesDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController
    with Logging {

  val form: Form[Boolean] = ReportedSharesDisposalListController.form(formProvider)

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
    onPageLoadCommon(srn, page, mode, Some(viewOnlyViewModel))(implicitly)
  }

  def onPageLoadCommon(srn: Srn, page: Int, mode: Mode, viewOnlyViewModel: Option[ViewOnlyViewModel] = None)(
    implicit request: DataRequest[AnyContent]
  ): Result =
    getCompletedDisposals(srn).map { completedDisposals =>
      if (viewOnlyViewModel.nonEmpty || completedDisposals.values.exists(_.nonEmpty)) {
        Ok(
          view(
            form,
            viewModel(
              srn,
              page,
              mode,
              completedDisposals,
              request.userAnswers,
              request.schemeDetails.schemeName,
              viewOnlyViewModel
            )
          )
        )
      } else {
        Redirect(routes.SharesDisposalController.onPageLoad(srn, NormalMode))
      }
    }.merge

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getCompletedDisposals(srn)
        .map { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfSharesItems = request.userAnswers.map(SharesCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems

          if (numberOfDisposals == maxPossibleNumberOfDisposals) {
            Redirect(
              navigator.nextPage(ReportedSharesDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors =>
                  BadRequest(
                    view(
                      errors,
                      viewModel(srn, page, mode, disposals, request.userAnswers, request.schemeDetails.schemeName)
                    )
                  ).pure[Future],
                reportAnotherDisposal =>
                  for {
                    updatedUserAnswers <- request.userAnswers
                      .setWhen(!reportAnotherDisposal)(SharesDisposalCompleted(srn), SectionCompleted)
                      .mapK[Future]
                    _ <- saveService.save(updatedUserAnswers)
                    submissionResult <- if (!reportAnotherDisposal) {
                      psrSubmissionService.submitPsrDetailsWithUA(
                        srn,
                        updatedUserAnswers,
                        fallbackCall = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                          .onPageLoad(srn, page)
                      )
                    } else {
                      Future.successful(Some(()))
                    }
                  } yield submissionResult.getOrRecoverJourney(
                    _ =>
                      Redirect(
                        navigator.nextPage(
                          ReportedSharesDisposalListPage(srn, reportAnotherDisposal),
                          mode,
                          request.userAnswers
                        )
                      )
                  )
              )
          }
        }
        .leftMap(_.pure[Future])
        .merge
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
          controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }

  private def getCompletedDisposals(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(SharesDisposalProgress.all(srn))
      .map {
        case (key, secondaryMap) =>
          key -> secondaryMap.filter { case (_, status) => status.completed }
      }
      .toList
      .traverse {
        case (key, sectionCompleted) =>
          for {
            sharesIndex <- refineStringIndex[Max5000.Refined](key).getOrRecoverJourney
            disposalIndexes <- sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)
          } yield (sharesIndex, disposalIndexes)
      }
      .map(_.toMap)
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
    if (disposals.isEmpty || mode.isViewOnlyMode) {
      List(
        ListRow.viewNoLink(
          Message("sharesDisposal.reportedSharesDisposalList.view.none", schemeName),
          "sharesDisposal.reportedSharesDisposalList.view.none.value"
        )
      )
    } else {
      disposals
        .flatMap {
          case (shareIndex, disposalIndexes) =>
            disposalIndexes.map { disposalIndex =>
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
        }
        .toList
        .sortBy(_.change.fold("")(_.url))
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
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers,
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None
  ): FormPageViewModel[ListViewModel] = {

    val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
    val numberOfSharesItems = userAnswers.map(SharesCompleted.all(srn)).size
    val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems


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
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals > 1 =>
        (
          "sharesDisposal.reportedSharesDisposalList.view.title",
          "sharesDisposal.reportedSharesDisposalList.view.heading"
        )
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals > 1 =>
        (
          "sharesDisposal.reportedSharesDisposalList.title.plural",
          "sharesDisposal.reportedSharesDisposalList.heading.plural"
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
      numberOfDisposals,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.ReportedSharesDisposalListController.onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          routes.ReportedSharesDisposalListController.onPageLoad(srn, _)
      }
    )

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxSharesTransactions) {
        Message("sharesDisposal.reportedSharesDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals) {
        ParagraphMessage("sharesDisposal.reportedSharesDisposalList.inset.allSharesDisposed.paragraph1") ++
          ParagraphMessage("sharesDisposal.reportedSharesDisposalList.inset.allSharesDisposed.paragraph2")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = Option.when(
        !((numberOfDisposals >= maxSharesTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals))
      )(
        ParagraphMessage("sharesDisposal.reportedSharesDisposalList.description")
      ),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, mode, disposals, userAnswers, viewOnlyViewModel, schemeName),
        Message("sharesDisposal.reportedSharesDisposalList.radios"),
        showRadios =
          !((numberOfDisposals >= maxSharesTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals)),
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
      }
    )
  }

  case class SharesDisposalData(
    shareIndex: Max5000,
    disposalIndex: Max50,
    sharesType: TypeOfShares,
    companyName: String,
    disposalMethod: HowSharesDisposed
  )
}
