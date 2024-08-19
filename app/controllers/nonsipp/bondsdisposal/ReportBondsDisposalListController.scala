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

package controllers.nonsipp.bondsdisposal

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds.{BondsCompleted, NameOfBondsPage}
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import cats.implicits._
import config.Constants.{maxBondsTransactions, maxDisposalPerBond}
import controllers.actions.IdentifyAndRequireData
import viewmodels.models.TaskListStatus.Updated
import models.HowDisposed.HowDisposed
import com.google.inject.Inject
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logging
import navigation.Navigator
import controllers.nonsipp.bondsdisposal.ReportBondsDisposalListController._
import forms.YesNoPageFormProvider
import utils.DateTimeUtils.localDateTimeShow
import models._
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal._
import viewmodels.DisplayMessage
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class ReportBondsDisposalListController @Inject()(
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

  val form: Form[Boolean] = ReportBondsDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, page, mode)
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
          pages.nonsipp.bondsdisposal.Paths.bondsDisposed
        ) == Updated
      } else {
        false
      },
      year = year,
      currentVersion = current,
      previousVersion = previous,
      compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
    )
    onPageLoadCommon(srn, page, mode, Some(viewOnlyViewModel))
  }

  def onPageLoadCommon(srn: Srn, page: Int, mode: Mode, viewOnlyViewModel: Option[ViewOnlyViewModel] = None)(
    implicit request: DataRequest[AnyContent]
  ): Result =
    getDisposals(srn).map { disposals =>
      val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfBondsItems = request.userAnswers.map(BondsCompleted.all(srn)).size
      val maxPossibleNumberOfDisposals = maxDisposalPerBond * numberOfBondsItems
      getBondsDisposalsWithIndexes(srn, disposals)
        .map(
          bondsDisposalsWithIndexes =>
            if (viewOnlyViewModel.nonEmpty || disposals.values.exists(_.nonEmpty)) {
              Ok(
                view(
                  form,
                  viewModel(
                    srn,
                    mode,
                    page,
                    bondsDisposalsWithIndexes,
                    numberOfDisposals,
                    maxPossibleNumberOfDisposals,
                    request.userAnswers,
                    request.schemeDetails.schemeName,
                    viewOnlyViewModel
                  )
                )
              )
            } else {
              Redirect(routes.BondsDisposalController.onPageLoad(srn, NormalMode))
            }
        )
        .merge
    }.merge

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getDisposals(srn)
        .traverse { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfBondsItems = request.userAnswers.map(BondsCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalPerBond * numberOfBondsItems
          if (numberOfDisposals == maxPossibleNumberOfDisposals) {
            Redirect(
              navigator.nextPage(ReportBondsDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors =>
                  getBondsDisposalsWithIndexes(srn, disposals)
                    .map(
                      indexes =>
                        BadRequest(
                          view(
                            errors,
                            viewModel(
                              srn,
                              mode,
                              page,
                              indexes,
                              numberOfDisposals,
                              maxPossibleNumberOfDisposals,
                              request.userAnswers,
                              request.schemeDetails.schemeName
                            )
                          )
                        )
                    )
                    .merge
                    .pure[Future],
                addAnotherDisposal =>
                  if (addAnotherDisposal) {
                    Redirect(
                      navigator
                        .nextPage(ReportBondsDisposalListPage(srn, addAnotherDisposal), mode, request.userAnswers)
                    ).pure[Future]
                  } else {
                    for {
                      updatedUserAnswers <- request.userAnswers
                        .set(BondsDisposalCompleted(srn), SectionCompleted)
                        .mapK[Future]
                      _ <- saveService.save(updatedUserAnswers)
                      submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                        srn,
                        updatedUserAnswers,
                        fallbackCall = controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
                          .onPageLoad(srn, page)
                      )
                    } yield submissionResult.getOrRecoverJourney(
                      _ =>
                        Redirect(
                          navigator
                            .nextPage(ReportBondsDisposalListPage(srn, addAnotherDisposal), mode, request.userAnswers)
                        )
                    )
                  }
              )
          }
        }
        .map(_.merge)
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
          controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }

  private def getDisposals(srn: Srn)(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(BondsDisposalProgress.all(srn))
      .filter(_._2.nonEmpty)
      .map {
        case (key, sectionCompleted) =>
          val maybeBondsIndex: Either[Result, Max5000] =
            refineStringIndex[Max5000.Refined](key).getOrRecoverJourney

          val maybeDisposalIndexes: Either[Result, List[Max50]] =
            sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)

          for {
            bondIndex <- maybeBondsIndex
            disposalIndexes <- maybeDisposalIndexes
          } yield (bondIndex, disposalIndexes)
      }
      .toList
      .sequence
      .map(_.toMap)

  private def getBondsDisposalsWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(
    implicit request: DataRequest[_]
  ): Either[Result, List[((Max5000, List[Max50]), SectionCompleted)]] =
    disposals
      .map {
        case indexes @ (index, _) =>
          index -> request.userAnswers
            .get(BondsCompleted(srn, index))
            .getOrRecoverJourney
            .map(bondsDisposal => (indexes, bondsDisposal))
      }
      .toList
      .sortBy { case (index, _) => index.value }
      .map { case (_, listRow) => listRow }
      .sequence
}

object ReportBondsDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "bondsDisposal.reportBondsDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    bondsDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    userAnswers: UserAnswers,
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    if (bondsDisposalsWithIndexes.isEmpty) {
      List(
        ListRow.viewNoLink(
          Message("bondsDisposal.reportBondsDisposalList.view.none", schemeName),
          "bondsDisposal.reportBondsDisposalList.view.none.value"
        )
      )
    } else {
      bondsDisposalsWithIndexes.flatMap {
        case ((bondIndex, disposalIndexes), bondsDisposal) =>
          disposalIndexes.map { disposalIndex =>
            val bondsDisposalData = BondsDisposalData(
              bondIndex,
              disposalIndex,
              userAnswers.get(NameOfBondsPage(srn, bondIndex)).get,
              userAnswers.get(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex)).get
            )

            (mode, viewOnlyViewModel) match {
              case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _))) =>
                ListRow.view(
                  buildMessage("bondsDisposal.reportBondsDisposalList.row", bondsDisposalData),
                  routes.BondsDisposalCYAController
                    .onPageLoadViewOnly(srn, bondIndex, disposalIndex, year, currentVersion, previousVersion)
                    .url,
                  buildMessage("bondsDisposal.reportBondsDisposalList.row.view.hidden", bondsDisposalData)
                )
              case (_, _) =>
                ListRow(
                  buildMessage("bondsDisposal.reportBondsDisposalList.row", bondsDisposalData),
                  changeUrl = routes.BondsDisposalCYAController
                    .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
                    .url,
                  changeHiddenText = buildMessage(
                    "bondsDisposal.reportBondsDisposalList.row.change.hidden",
                    bondsDisposalData
                  ),
                  removeUrl = routes.RemoveBondsDisposalController
                    .onPageLoad(srn, bondIndex, disposalIndex)
                    .url,
                  removeHiddenText = buildMessage(
                    "bondsDisposal.reportBondsDisposalList.row.remove.hidden",
                    bondsDisposalData
                  )
                )
            }
          }
      }
    }

  private def buildMessage(messageString: String, bondsDisposalData: BondsDisposalData): Message =
    bondsDisposalData match {
      case BondsDisposalData(_, _, nameOfBonds, typeOfDisposal) =>
        val disposalType = typeOfDisposal match {
          case HowDisposed.Sold => "bondsDisposal.reportBondsDisposalList.methodOfDisposal.sold"
          case HowDisposed.Transferred => "bondsDisposal.reportBondsDisposalList.methodOfDisposal.transferred"
          case HowDisposed.Other(_) => "bondsDisposal.reportBondsDisposalList.methodOfDisposal.other"
        }
        Message(messageString, nameOfBonds, disposalType)
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    bondsDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int,
    userAnswers: UserAnswers,
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = ((mode, numberOfDisposals) match {
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals == 0 =>
        ("bondsDisposal.reportBondsDisposalList.view.title", "bondsDisposal.reportBondsDisposalList.view.heading.none")

      case (ViewOnlyMode, _) if numberOfDisposals > 1 =>
        (
          "bondsDisposal.reportBondsDisposalList.view.title.plural",
          "bondsDisposal.reportBondsDisposalList.view.heading.plural"
        )
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals > 1 =>
        (
          "bondsDisposal.reportBondsDisposalList.view.title",
          "bondsDisposal.reportBondsDisposalList.view.heading"
        )
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals > 1 =>
        (
          "bondsDisposal.reportBondsDisposalList.title.plural",
          "bondsDisposal.reportBondsDisposalList.heading.plural"
        )
      case _ =>
        (
          "bondsDisposal.reportBondsDisposalList.title",
          "bondsDisposal.reportBondsDisposalList.heading"
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
          routes.ReportBondsDisposalListController.onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          routes.ReportBondsDisposalListController.onPageLoad(srn, _)
      }
    )

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxBondsTransactions) {
        Message("bondsDisposal.reportBondsDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals) {
        ParagraphMessage("bondsDisposal.reportBondsDisposalList.inset.allBondsDisposed.paragraph1") ++
          ParagraphMessage("bondsDisposal.reportBondsDisposalList.inset.allBondsDisposed.paragraph2")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = Option.when(
        !((numberOfDisposals >= maxBondsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals))
      )(
        ParagraphMessage("bondsDisposal.reportBondsDisposalList.description")
      ),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, mode, bondsDisposalsWithIndexes, userAnswers, viewOnlyViewModel, schemeName),
        Message("bondsDisposal.reportBondsDisposalList.radios"),
        showRadios =
          !((numberOfDisposals >= maxBondsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals)),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "bondsDisposal.reportBondsDisposalList.pagination.label",
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
      onSubmit = routes.ReportBondsDisposalListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "bondsDisposal.reportBondsDisposalList.view.link",
                controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
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
              controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
                .onSubmit(srn, page, mode)
          }
        )
      }
    )
  }

  case class BondsDisposalData(
    bondIndex: Max5000,
    disposalIndex: Max50,
    nameOfBonds: String,
    disposalMethod: HowDisposed
  )
}
