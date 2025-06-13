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

package controllers.nonsipp.shares

import services.SaveService
import viewmodels.implicits._
import com.google.inject.Inject
import utils.ListUtils._
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import utils.IntUtils.toInt
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.shares._
import play.api.mvc._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.ListView
import models.SchemeId.Srn
import cats.implicits.{catsSyntaxApplicativeId, toShow, toTraverseOps}
import controllers.nonsipp.shares.SharesListController._
import _root_.config.Constants
import config.Constants.maxSharesTransactions
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import navigation.Navigator
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import utils.nonsipp.check.SharesCheckStatusUtils
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import utils.MapUtils.UserAnswersMapOps
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.Named

class SharesListController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  given logger: Logger = Logger(getClass)

  val form: Form[Boolean] = SharesListController.form(formProvider)

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
            pages.nonsipp.shares.Paths.shareTransactions
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
    val indexes: List[Max5000] = request.userAnswers.map(SharesCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty || mode.isViewOnlyMode) {
      shares(srn).map { case (sharesToCheck, shares) =>
        val filledForm =
          request.userAnswers.get(SharesListPage(srn)).fold(form)(form.fill)
        Ok(
          view(
            filledForm,
            viewModel(
              srn,
              page,
              mode,
              shares,
              sharesToCheck,
              request.schemeDetails.schemeName,
              viewOnlyViewModel,
              showBackLink = showBackLink,
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
      val inProgressAnswers = request.userAnswers.map(SharesProgress.all(srn))
      val inProgressUrl = inProgressAnswers.collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }

      shares(srn).traverse { case (sharesToCheck, shares) =>
        if (sharesToCheck.size + shares.size >= Constants.maxSharesTransactions) {
          Future.successful(
            Redirect(
              navigator.nextPage(SharesListPage(srn), mode, request.userAnswers)
            )
          )
        } else {
          form
            .bindFromRequest()
            .fold(
              errors =>
                BadRequest(
                  view(
                    errors,
                    viewModel(
                      srn,
                      page,
                      mode,
                      shares,
                      sharesToCheck,
                      request.schemeDetails.schemeName,
                      None,
                      showBackLink = true,
                      isPrePopulation
                    )
                  )
                ).pure[Future],
              addAnother =>
                for {
                  updatedUserAnswers <- Future.fromTry(request.userAnswers.set(SharesListPage(srn), addAnother))
                  _ <- saveService.save(updatedUserAnswers)
                } yield (addAnother, inProgressUrl) match {
                  case (true, Some(url)) => Redirect(url)
                  case _ =>
                    Redirect(
                      navigator.nextPage(SharesListPage(srn), mode, updatedUserAnswers)
                    )
                }
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
              pages.nonsipp.shares.Paths.shareTransactions
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

  private def shares(srn: Srn)(implicit
    request: DataRequest[_],
    logger: Logger
  ): Either[Result, (List[SharesData], List[SharesData])] = {
    // if return has been pre-populated, partition shares by those that need to be checked
    def buildShares(index: Max5000): Either[Result, SharesData] =
      for {
        typeOfSharesHeld <- requiredPage(TypeOfSharesHeldPage(srn, index))
        companyName <- requiredPage(CompanyNameRelatedSharesPage(srn, index))
        acquisitionType <- requiredPage(WhyDoesSchemeHoldSharesPage(srn, index))
        acquisitionDate = request.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index))
        canRemove = request.userAnswers.get(SharePrePopulated(srn, index)).isEmpty
      } yield SharesData(index, typeOfSharesHeld, companyName, acquisitionType, acquisitionDate, canRemove)

    val completedIndexes = request.userAnswers.map(SharesProgress.all(srn)).filter(_._2.completed).keys.toList
    if (isPrePopulation) {
      for {
        indexes <- request.userAnswers
          .map(TypeOfSharesHeldPages(srn))
          .collect { case (index, address) if completedIndexes.contains(index) => (index, address) }
          .refine[Max5000.Refined]
          .map(_.keys.toList)
          .getOrRecoverJourney
        shares <- indexes.traverse(buildShares)
      } yield shares.partition(shares =>
        SharesCheckStatusUtils.checkSharesRecord(request.userAnswers, srn, shares.index)
      )
    } else {
      val noSharesToCheck = List.empty[SharesData]
      for {
        indexes <- request.userAnswers
          .map(TypeOfSharesHeldPages(srn))
          .collect { case (index, address) if completedIndexes.contains(index) => (index, address) }
          .refine[Max5000.Refined]
          .map(_.keys.toList)
          .getOrRecoverJourney
        shares <- indexes.traverse(buildShares)
      } yield (noSharesToCheck, shares)
    }
  }
}

object SharesListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "sharesList.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    sharesList: List[SharesData],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String,
    check: Boolean = false
  ): List[ListRow] =
    (sharesList, mode) match {
      case (Nil, mode) if mode.isViewOnlyMode =>
        List(
          ListRow.viewNoLink(
            Message("sharesList.view.none", schemeName),
            "sharesList.view.none.value"
          )
        )
      case (Nil, mode) if !mode.isViewOnlyMode =>
        List()
      case (list, _) =>
        list.map { case SharesData(index, typeOfShares, companyName, acquisition, acquisitionDate, canRemove) =>
          val sharesType = typeOfShares match {
            case TypeOfShares.SponsoringEmployer => "sharesList.sharesType.sponsoringEmployer"
            case TypeOfShares.Unquoted => "sharesList.sharesType.unquoted"
            case TypeOfShares.ConnectedParty => "sharesList.sharesType.connectedParty"
          }
          val acquisitionType = acquisition match {
            case SchemeHoldShare.Acquisition => "sharesList.acquisition.acquired"
            case SchemeHoldShare.Contribution => "sharesList.acquisition.contributed"
            case SchemeHoldShare.Transfer => "sharesList.acquisition.transferred"
          }
          val sharesMessage = acquisitionDate match {
            case Some(date) => Message("sharesList.row.withDate", sharesType, companyName, acquisitionType, date.show)
            case None => Message("sharesList.row", sharesType, companyName, acquisitionType)
          }

          (mode, viewOnlyViewModel) match {
            case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
              ListRow.view(
                sharesMessage,
                routes.SharesCYAController
                  .onPageLoadViewOnly(srn, index, year, current, previous)
                  .url,
                Message("site.view.param", sharesMessage)
              )
            case _ if check =>
              ListRow.check(
                sharesMessage,
                routes.SharesCheckAndUpdateController.onPageLoad(srn, index).url,
                Message("site.check.param", sharesMessage)
              )
            case _ if canRemove =>
              ListRow(
                sharesMessage,
                changeUrl = routes.SharesCYAController.onPageLoad(srn, index, CheckMode).url,
                changeHiddenText = Message("site.change.param", sharesMessage),
                removeUrl = routes.RemoveSharesController.onPageLoad(srn, index, mode).url,
                removeHiddenText = Message("site.remove.param", sharesMessage)
              )
            case _ =>
              ListRow(
                sharesMessage,
                changeUrl = routes.SharesCYAController.onPageLoad(srn, index, CheckMode).url,
                changeHiddenText = Message("site.change.param", sharesMessage)
              )
          }
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    shares: List[SharesData],
    sharesToCheck: List[SharesData],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    isPrePop: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val sharesSize = if (isPrePop) shares.length + sharesToCheck.size else shares.length

    val (title, heading) = (mode match {
      // View only
      case ViewOnlyMode if sharesSize == 0 =>
        ("sharesList.view.title.none", "sharesList.view.heading.none")
      case ViewOnlyMode if sharesSize > 1 =>
        ("sharesList.view.title.plural", "sharesList.view.heading.plural")
      case ViewOnlyMode =>
        ("sharesList.view.title", "sharesList.view.heading")
      // Pre-pop
      case _ if isPrePop && shares.nonEmpty =>
        ("sharesList.title.prepop.check", "sharesList.heading.prepop.check")
      case _ if isPrePop && sharesSize > 1 =>
        ("sharesList.title.prepop.plural", "sharesList.heading.prepop.plural")
      case _ if isPrePop =>
        ("sharesList.title.prepop", "sharesList.heading.prepop")
      // Normal
      case _ if sharesSize > 1 =>
        ("sharesList.title.plural", "sharesList.heading.plural")
      case _ =>
        ("sharesList.title", "sharesList.heading")
    }) match {
      case (title, heading) =>
        (Message(title, sharesSize), Message(heading, sharesSize))
    }

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.pageSize >= sharesSize) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.pageSize,
      totalSize = sharesSize,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.SharesListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.SharesListController.onPageLoad(srn, _, NormalMode)
      }
    )

    val paragraph = Option.when(sharesSize < maxSharesTransactions) {
      if (sharesToCheck.nonEmpty) {
        ParagraphMessage("sharesList.description.prepop") ++
          ParagraphMessage("sharesList.description.disposal")
      } else {
        ParagraphMessage("sharesList.description") ++
          ParagraphMessage("sharesList.description.disposal")
      }
    }

    val conditionalInsetText: DisplayMessage =
      if (sharesSize >= Constants.maxSharesTransactions) {
        ParagraphMessage("sharesList.inset")
      } else {
        Message("")
      }

    val sections =
      if (isPrePop) {
        Option
          .when(sharesToCheck.nonEmpty)(
            ListSection(
              heading = Some("sharesList.section.check"),
              rows(srn, mode, sharesToCheck, viewOnlyViewModel, schemeName, check = true)
            )
          )
          .toList ++
          Option
            .when(shares.nonEmpty)(
              ListSection(
                heading = Some("sharesList.section.added"),
                rows(srn, mode, shares, viewOnlyViewModel, schemeName)
              )
            )
            .toList
      } else {
        List(
          ListSection(rows(srn, mode, shares, viewOnlyViewModel, schemeName))
        )
      }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = paragraph,
      page = ListViewModel(
        inset = conditionalInsetText,
        sections = sections,
        radioText = Message("sharesList.radios"),
        showRadios = sharesSize < Constants.maxSharesTransactions,
        showInsetWithRadios = !(sharesSize < Constants.maxSharesTransactions),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "sharesList.pagination.label",
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
      onSubmit = controllers.nonsipp.shares.routes.SharesListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "sharesList.view.link",
                controllers.nonsipp.shares.routes.SharesListController
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
          onSubmit = controllers.nonsipp.shares.routes.SharesListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }

  case class SharesData(
    index: Max5000,
    typeOfShares: TypeOfShares,
    companyName: String,
    acquisitionType: SchemeHoldShare,
    acquisitionDate: Option[LocalDate],
    canRemove: Boolean
  )
}
