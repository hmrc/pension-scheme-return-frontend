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

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import com.google.inject.Inject
import utils.ListUtils._
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.shares._
import play.api.mvc._
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import cats.implicits.{catsSyntaxApplicativeId, toShow, toTraverseOps}
import controllers.nonsipp.shares.SharesListController._
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.Named

class SharesListController @Inject()(
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

  val form: Form[Boolean] = SharesListController.form(formProvider)

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
    val indexes: List[Max5000] = request.userAnswers.map(SharesCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty) {
      sharesData(srn, indexes).map { data =>
        val filledForm =
          request.userAnswers.get(SharesListPage(srn)).fold(form)(form.fill)
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
                  pages.nonsipp.shares.Paths.shareTransactions
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
      val indexes: List[Max5000] = request.userAnswers.map(SharesCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.size >= Constants.maxSharesTransactions) {
        Future.successful(
          Redirect(
            navigator.nextPage(SharesListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors => {
              sharesData(srn, indexes)
                .map { data =>
                  BadRequest(view(errors, viewModel(srn, page, mode, data, false, None, None, None)))
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
                        SharesJourneyStatus(srn),
                        if (!addAnother) SectionStatus.Completed else SectionStatus.InProgress
                      )
                      .set(SharesListPage(srn), addAnother)
                  )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (!addAnother) {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall = controllers.nonsipp.shares.routes.SharesListController.onPageLoad(srn, page, mode)
                  )
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(SharesListPage(srn), mode, updatedUserAnswers)
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
          controllers.nonsipp.shares.routes.SharesListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }

  private def sharesData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[SharesData]] =
    indexes
      .sortBy(listRow => listRow.value)
      .map { index =>
        for {
          typeOfSharesHeld <- requiredPage(TypeOfSharesHeldPage(srn, index))
          companyName <- requiredPage(CompanyNameRelatedSharesPage(srn, index))
          acquisitionType <- requiredPage(WhyDoesSchemeHoldSharesPage(srn, index))
          acquisitionDate = req.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index))
        } yield SharesData(index, typeOfSharesHeld, companyName, acquisitionType, acquisitionDate)
      }
      .sequence
}

object SharesListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "sharesList.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[SharesData],
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[ListRow] =
    memberList.map {
      case SharesData(index, typeOfShares, companyName, acquisition, acquisitionDate) =>
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

        if (mode.isViewOnlyMode) {
          (mode, optYear, optCurrentVersion, optPreviousVersion) match {
            case (ViewOnlyMode, Some(year), Some(current), Some(previous)) =>
              ListRow.view(
                sharesMessage,
                controllers.nonsipp.shares.routes.SharesCYAController
                  .onPageLoadViewOnly(srn, index, year, current, previous)
                  .url,
                Message("sharesList.row.change.hiddenText", sharesMessage)
              )
          }
        } else {
          ListRow(
            sharesMessage,
            changeUrl = controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode).url,
            changeHiddenText = Message("sharesList.row.change.hiddenText", sharesMessage),
            removeUrl = routes.RemoveSharesController.onPageLoad(srn, index, mode).url,
            removeHiddenText = Message("sharesList.row.remove.hiddenText", sharesMessage)
          )
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[SharesData],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ListViewModel] = {

    val lengthOfData = data.length

    val (title, heading) = ((mode, lengthOfData) match {
      case (ViewOnlyMode, lengthOfData) if lengthOfData > 1 =>
        ("sharesList.view.title.plural", "sharesList.view.heading.plural")
      case (ViewOnlyMode, _) =>
        ("sharesList.view.title", "sharesList.view.heading")
      case (_, lengthOfData) if lengthOfData > 1 =>
        ("sharesList.title.plural", "sharesList.heading.plural")
      case _ =>
        ("sharesList.title", "sharesList.heading")
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
          routes.SharesListController.onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.SharesListController.onPageLoad(srn, _, NormalMode)
      }
    )

    val conditionalInsetText: DisplayMessage = {
      if (data.size >= Constants.maxSharesTransactions) {
        ParagraphMessage("sharesList.inset")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = Some(ParagraphMessage("sharesList.description")),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows = rows(srn, mode, data, optYear, optCurrentVersion, optPreviousVersion),
        radioText = Message("sharesList.radios"),
        showRadios = data.size < Constants.maxSharesTransactions,
        showInsetWithRadios = !(data.length < Constants.maxSharesTransactions),
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
      optViewOnlyDetails = Option.when(mode.isViewOnlyMode) {
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if (optYear.nonEmpty && currentVersion > 1 && previousVersion > 0) =>
              Some(
                LinkMessage(
                  "sharesList.view.link",
                  controllers.nonsipp.shares.routes.SharesListController
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
              controllers.nonsipp.shares.routes.SharesListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.shares.routes.SharesListController
                .onSubmit(srn, page, mode)
          }
        )
      }
    )
  }

  case class SharesData(
    index: Max5000,
    typeOfShares: TypeOfShares,
    companyName: String,
    acquisitionType: SchemeHoldShare,
    acquisitionDate: Option[LocalDate]
  )
}
