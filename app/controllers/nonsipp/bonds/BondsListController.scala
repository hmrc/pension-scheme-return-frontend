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

package controllers.nonsipp.bonds

import services.SaveService
import pages.nonsipp.bonds._
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.ListUtils._
import utils.IntUtils.{toInt, toRefined5000}
import cats.implicits.{catsSyntaxApplicativeId, toShow, toTraverseOps}
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import config.RefinedTypes.Max5000
import controllers.PSRController
import controllers.nonsipp.bonds.BondsListController._
import utils.nonsipp.TaskListStatusUtils.{getBondsTaskListStatusAndLink, getCompletedOrUpdatedTaskListStatus}
import views.html.ListView
import models.SchemeId.Srn
import _root_.config.Constants
import config.Constants.maxBondsTransactions
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import utils.nonsipp.check.BondsCheckStatusUtils
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import utils.MapUtils.UserAnswersMapOps
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class BondsListController @Inject() (
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

  val form: Form[Boolean] = BondsListController.form(formProvider)

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
            pages.nonsipp.bonds.Paths.bondTransactions,
            Some("bondsDisposed")
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
  )(implicit
    request: DataRequest[AnyContent]
  ): Result = {
    val (status, incompleteBondsUrl) =
      getBondsTaskListStatusAndLink(request.userAnswers, srn, isPrePopulation)

    if (status == TaskListStatus.NotStarted) {
      logger.info("Bonds journey not started, redirecting to NameOfBonds page")
      Redirect(routes.NameOfBondsController.onPageLoad(srn, 1, NormalMode))
    } else if (status == TaskListStatus.InProgress) {
      Redirect(incompleteBondsUrl)
    } else {
      bondsData(srn).map { case (bondsToCheck, bonds) =>
        val filledForm =
          request.userAnswers.get(BondsListPage(srn)).fold(form)(form.fill)
        Ok(
          view(
            filledForm,
            viewModel(
              srn,
              page,
              mode,
              bonds,
              bondsToCheck,
              request.schemeDetails.schemeName,
              viewOnlyViewModel,
              showBackLink = showBackLink,
              isPrePop = isPrePopulation
            )
          )
        )
      }.merge
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val indexes: List[Max5000] = request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]
      val inProgressUrl = request.userAnswers.map(BondsProgress.all(srn)).collectFirst {
        case (_, SectionJourneyStatus.InProgress(url)) => url
      }

      if (indexes.size >= Constants.maxBondsTransactions) {
        Future.successful(
          Redirect(
            navigator.nextPage(BondsListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              bondsData(srn)
                .map { case (bondsToCheck, bonds) =>
                  BadRequest(
                    view(
                      errors,
                      viewModel(
                        srn,
                        page,
                        mode,
                        bonds,
                        bondsToCheck,
                        request.schemeDetails.schemeName,
                        showBackLink = true,
                        isPrePop = isPrePopulation
                      )
                    )
                  )
                }
                .merge
                .pure[Future],
            addAnother =>
              for {
                updatedUserAnswers <- Future.fromTry(request.userAnswers.set(BondsListPage(srn), addAnother))
                _ <- saveService.save(updatedUserAnswers)
              } yield (addAnother, inProgressUrl) match {
                case (true, Some(url)) => Redirect(url)
                case _ => Redirect(navigator.nextPage(BondsListPage(srn), mode, updatedUserAnswers))
              }
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
              pages.nonsipp.bonds.Paths.bondTransactions,
              Some("bondsDisposed")
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

  private def bondsData(srn: Srn)(implicit
    request: DataRequest[?],
    logger: Logger
  ): Either[Result, (List[BondsData], List[BondsData])] = {
    // if return has been pre-populated, partition bonds by those that need to be checked
    def buildBonds(index: Max5000): Either[Result, BondsData] =
      for {
        nameOfBonds <- requiredPage(NameOfBondsPage(srn, index))
        acquisitionType <- requiredPage(WhyDoesSchemeHoldBondsPage(srn, index))
        costOfBonds <- requiredPage(CostOfBondsPage(srn, index))
        canRemove = request.userAnswers.get(BondPrePopulated(srn, index)).isEmpty
      } yield BondsData(index, nameOfBonds, acquisitionType, costOfBonds, canRemove)

    val completedIndexesOrError = for {
      indexes <- request.userAnswers
        .map(BondsProgress.all(srn))
        .refine[Max5000.Refined]
        .getOrRecoverJourney
      completedIndexes = indexes
        .filter { case (_, progress) => progress.completed }
        .map { case (key, _) => key }
        .toList
    } yield completedIndexes

    if (isPrePopulation) {
      for {
        completedIndexes <- completedIndexesOrError
        bonds <- completedIndexes.traverse(buildBonds)
      } yield bonds.partition(bonds => BondsCheckStatusUtils.checkBondsRecord(request.userAnswers, srn, bonds.index))
    } else {
      val noBondsToCheck = List.empty[BondsData]
      for {
        completedIndexes <- completedIndexesOrError
        bonds <- completedIndexes.traverse(buildBonds)
      } yield (noBondsToCheck, bonds)
    }
  }
}

object BondsListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "bondsList.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    bondsList: List[BondsData],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String,
    check: Boolean = false
  ): List[ListRow] =
    (bondsList, mode) match {
      case (Nil, mode) if mode.isViewOnlyMode =>
        List(
          ListRow.viewNoLink(
            Message("bondsList.view.none", schemeName),
            "bondsList.view.none.value"
          )
        )
      case (Nil, mode) if !mode.isViewOnlyMode =>
        List()
      case (list, _) =>
        list.map { case BondsData(index, nameOfBonds, acquisition, costOfBonds, canRemove) =>
          val acquisitionType = acquisition match {
            case SchemeHoldBond.Acquisition => "bondsList.acquisition.acquired"
            case SchemeHoldBond.Contribution => "bondsList.acquisition.contributed"
            case SchemeHoldBond.Transfer => "bondsList.acquisition.transferred"
          }
          val bondsMessage =
            Message("bondsList.row.withCost", nameOfBonds.show, acquisitionType, costOfBonds.displayAs)

          (mode, viewOnlyViewModel) match {
            case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
              ListRow.view(
                bondsMessage,
                controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                  .onPageLoadViewOnly(srn, index, year, current, previous)
                  .url,
                Message("bondsList.row.view.hiddenText", bondsMessage)
              )
            case _ if check =>
              ListRow.check(
                bondsMessage,
                routes.BondsCheckAndUpdateController.onPageLoad(srn, index).url,
                Message("bondsList.row.check.hiddenText", bondsMessage)
              )
            case _ if canRemove =>
              ListRow(
                bondsMessage,
                changeUrl = controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                  .onPageLoad(srn, index, CheckMode)
                  .url,
                changeHiddenText = Message("bondsList.row.change.hiddenText", bondsMessage),
                removeUrl = controllers.nonsipp.bonds.routes.RemoveBondsController
                  .onPageLoad(srn, index, NormalMode)
                  .url,
                removeHiddenText = Message("bondsList.row.remove.hiddenText", bondsMessage)
              )

            case _ =>
              ListRow(
                bondsMessage,
                changeUrl = controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                  .onPageLoad(srn, index, CheckMode)
                  .url,
                changeHiddenText = Message("bondsList.row.change.hiddenText", bondsMessage)
              )

          }
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    bonds: List[BondsData],
    bondsToCheck: List[BondsData],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    isPrePop: Boolean
  ): FormPageViewModel[ListViewModel] = {
    val bondsSize = if (isPrePop) bonds.length + bondsToCheck.size else bonds.length

    val (title, heading) = (mode match {
      // View only
      case ViewOnlyMode if bondsSize == 0 =>
        ("bondsList.view.title", "bondsList.view.heading.none")
      case ViewOnlyMode if bondsSize > 1 =>
        ("bondsList.view.title.plural", "bondsList.view.heading.plural")
      case ViewOnlyMode =>
        ("bondsList.view.title", "bondsList.view.heading")
      // Pre-pop
      case _ if isPrePop && bonds.nonEmpty =>
        ("bondsList.title.prepop.check", "bondsList.heading.prepop.check")
      case _ if isPrePop && bondsSize > 1 =>
        ("bondsList.title.prepop.plural", "bondsList.heading.prepop.plural")
      case _ if isPrePop =>
        ("bondsList.title.prepop", "bondsList.heading.prepop")
      // Normal
      case _ if bondsSize > 1 =>
        ("bondsList.title.plural", "bondsList.heading.plural")
      case _ =>
        ("bondsList.title", "bondsList.heading")
    }) match {
      case (title, heading) =>
        (Message(title, bondsSize), Message(heading, bondsSize))
    }

    val currentPage = if ((page - 1) * Constants.pageSize >= bondsSize) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.pageSize,
      totalSize = bondsSize,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          controllers.nonsipp.bonds.routes.BondsListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          controllers.nonsipp.bonds.routes.BondsListController.onPageLoad(srn, _, mode)
      }
    )

    val conditionalInsetText: DisplayMessage =
      if (bondsSize >= Constants.maxBondsTransactions) {
        ParagraphMessage("bondsList.inset")
      } else {
        Message("")
      }

    val sections: List[ListSection] = if (isPrePop) {
      Option
        .when(bondsToCheck.nonEmpty)(
          ListSection(
            heading = Some("bondsList.section.check"),
            rows(srn, mode, bondsToCheck, viewOnlyViewModel, schemeName, check = true)
          )
        )
        .toList ++
        Option
          .when(bonds.nonEmpty)(
            ListSection(
              heading = Some("bondsList.section.added"),
              rows(srn, mode, bonds, viewOnlyViewModel, schemeName)
            )
          )
          .toList
    } else {
      List(
        ListSection(rows(srn, mode, bonds, viewOnlyViewModel, schemeName))
      )
    }

    val paragraph = Option.when(bondsSize < maxBondsTransactions) {
      if (bondsToCheck.nonEmpty) {
        ParagraphMessage("bondsList.description.prepop.check")
      } else if (isPrePop) {
        ParagraphMessage("bondsList.description.prepop")
      } else {
        ParagraphMessage("bondsList.description")
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
        radioText = Message(if (isPrePop) "bondsList.radios.prepop" else "bondsList.radios"),
        showRadios = bondsSize < Constants.maxBondsTransactions,
        showInsetWithRadios = !(bondsSize < Constants.maxBondsTransactions),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "bondsList.pagination.label",
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
      onSubmit = controllers.nonsipp.bonds.routes.BondsListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "bondsList.view.link",
                controllers.nonsipp.bonds.routes.BondsListController
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
          onSubmit = controllers.nonsipp.bonds.routes.BondsListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }

  case class BondsData(
    index: Int,
    nameOfBonds: String,
    acquisitionType: SchemeHoldBond,
    costOfBonds: Money,
    canRemove: Boolean
  )

}
