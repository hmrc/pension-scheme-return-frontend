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

import services.SaveService
import pages.nonsipp.bonds.{BondsCompleted, NameOfBondsPage}
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import utils.IntUtils.toInt
import cats.implicits._
import _root_.config.Constants
import controllers.actions.IdentifyAndRequireData
import _root_.config.Constants.{maxBondsTransactions, maxDisposalPerBond}
import viewmodels.models.TaskListStatus.Updated
import _root_.config.RefinedTypes.{Max50, Max5000}
import models.HowDisposed.HowDisposed
import com.google.inject.Inject
import views.html.ListView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
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

class ReportBondsDisposalListController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val logger = Logger(getClass)

  val form: Form[Boolean] = ReportBondsDisposalListController.form(formProvider)

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
      val numberOfBondsItems = request.userAnswers.map(BondsCompleted.all()).size
      val maxPossibleNumberOfDisposals = maxDisposalPerBond * numberOfBondsItems

      getBondsDisposalsWithIndexes(srn, completedDisposals).map { bondsDisposalsWithIndexes =>
        val allBondsFullyDisposed: Boolean = bondsDisposalsWithIndexes.forall {
          case ((bondIndex, disposalIndexes), _) =>
            disposalIndexes.exists { disposalIndex =>
              request.userAnswers.get(BondsStillHeldPage(srn, bondIndex, disposalIndex)).contains(0)
            }
        }

        val maximumDisposalsReached = numberOfDisposals >= maxBondsTransactions * maxDisposalPerBond ||
          numberOfDisposals >= maxPossibleNumberOfDisposals ||
          allBondsFullyDisposed

        logger.debug(s"All Bonds Fully Disposed: $allBondsFullyDisposed")
        logger.debug(s"Maximum Disposals Reached: $maximumDisposalsReached")

        if (viewOnlyViewModel.nonEmpty || completedDisposals.values.exists(_.nonEmpty)) {
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
                viewOnlyViewModel,
                showBackLink = showBackLink,
                maximumDisposalsReached = maximumDisposalsReached,
                allBondsFullyDisposed = allBondsFullyDisposed
              )
            )
          )
        } else {
          logger.info("No completed bond disposal, start a new one")
          Redirect(routes.BondsDisposalController.onPageLoad(srn, NormalMode))
        }
      }.merge
    }.merge

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getCompletedDisposals()
        .traverse { completedDisposals =>
          val numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfBondsItems = request.userAnswers.map(BondsCompleted.all()).size
          val maxPossibleNumberOfDisposals = maxDisposalPerBond * numberOfBondsItems

          val allBondsFullyDisposed: Boolean = completedDisposals.forall { case (bondIndex, disposalIndexes) =>
            disposalIndexes.exists { disposalIndex =>
              request.userAnswers.get(BondsStillHeldPage(srn, bondIndex, disposalIndex)).contains(0)
            }
          }

          val maximumDisposalsReached = numberOfDisposals >= maxBondsTransactions * maxDisposalPerBond ||
            numberOfDisposals >= maxPossibleNumberOfDisposals ||
            allBondsFullyDisposed

          if (maximumDisposalsReached) {
            Redirect(
              navigator.nextPage(ReportBondsDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors =>
                  getBondsDisposalsWithIndexes(srn, completedDisposals)
                    .map { indexes =>
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
                            request.schemeDetails.schemeName,
                            viewOnlyViewModel = None,
                            showBackLink = true,
                            maximumDisposalsReached = maximumDisposalsReached,
                            allBondsFullyDisposed = allBondsFullyDisposed
                          )
                        )
                      )
                    }
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
                    } yield Redirect(
                      navigator
                        .nextPage(ReportBondsDisposalListPage(srn, addAnotherDisposal), mode, request.userAnswers)
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
              pages.nonsipp.bondsdisposal.Paths.bondsDisposed
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

  private def getCompletedDisposals()(implicit request: DataRequest[?]): Either[Result, Map[Max5000, List[Max50]]] =
    Right(
      request.userAnswers
        .map(BondsCompleted.all())
        .keys
        .toList
        .refine[Max5000.Refined]
        .map { index =>
          index -> request.userAnswers
            .map(BondsDisposalProgress.all(index))
            .filter(_._2.completed)
            .keys
            .toList
            .refine[Max50.Refined]
        }
        .toMap
    )

  private def getBondsDisposalsWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(implicit
    request: DataRequest[?]
  ): Either[Result, List[((Max5000, List[Max50]), SectionCompleted)]] =
    disposals
      .map { case indexes @ (index, _) =>
        index -> request.userAnswers
          .get(BondsCompleted(srn, index))
          .toRight {
            logger.warn(s"couldn't find completed bonds page for index ${index} from bonds disposals completed")
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
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
      bondsDisposalsWithIndexes.flatMap { case ((bondIndex, disposalIndexes), _) =>
        disposalIndexes.sortBy(_.value).map { disposalIndex =>
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
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    maximumDisposalsReached: Boolean,
    allBondsFullyDisposed: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = ((mode, numberOfDisposals) match {
      case (ViewOnlyMode, num) if num == 0 =>
        ("bondsDisposal.reportBondsDisposalList.view.title", "bondsDisposal.reportBondsDisposalList.view.heading.none")

      case (ViewOnlyMode, num) if num > 1 =>
        (
          "bondsDisposal.reportBondsDisposalList.view.title.plural",
          "bondsDisposal.reportBondsDisposalList.view.heading.plural"
        )
      case (ViewOnlyMode, _) =>
        (
          "bondsDisposal.reportBondsDisposalList.view.title",
          "bondsDisposal.reportBondsDisposalList.view.heading"
        )
      case (_, num) if num > 1 =>
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
      totalSize = numberOfDisposals,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.ReportBondsDisposalListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          routes.ReportBondsDisposalListController.onPageLoad(srn, _)
      }
    )

    val conditionalInsetText: DisplayMessage =
      if (numberOfDisposals >= maxBondsTransactions * maxDisposalPerBond) {
        Message("bondsDisposal.reportBondsDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals || allBondsFullyDisposed) {
        ParagraphMessage("bondsDisposal.reportBondsDisposalList.inset.allBondsDisposed.paragraph1") ++
          ParagraphMessage("bondsDisposal.reportBondsDisposalList.inset.allBondsDisposed.paragraph2")
      } else {
        Message("")
      }

    val showRadios = !maximumDisposalsReached && !mode.isViewOnlyMode &&
      numberOfDisposals < maxPossibleNumberOfDisposals && !allBondsFullyDisposed

    val description = Option.when(
      !maximumDisposalsReached && !allBondsFullyDisposed
    )(
      ParagraphMessage("bondsDisposal.reportBondsDisposalList.description")
    )

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = description,
      page = ListViewModel(
        inset = conditionalInsetText,
        sections =
          List(ListSection(rows(srn, mode, bondsDisposalsWithIndexes, userAnswers, viewOnlyViewModel, schemeName))),
        Message("bondsDisposal.reportBondsDisposalList.radios"),
        showRadios = showRadios,
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
      },
      showBackLink = showBackLink
    )
  }

  case class BondsDisposalData(
    bondIndex: Max5000,
    disposalIndex: Max50,
    nameOfBonds: String,
    disposalMethod: HowDisposed
  )
}
