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

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds._
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.ListUtils._
import config.Refined.Max5000
import controllers.PSRController
import cats.implicits.{catsSyntaxApplicativeId, toShow, toTraverseOps}
import _root_.config.Constants
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import controllers.nonsipp.bonds.BondsListController._
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
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

class BondsListController @Inject()(
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

  val form: Form[Boolean] = BondsListController.form(formProvider)

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
    val indexes: List[Max5000] = request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty) {
      bondsData(srn, indexes).map { data =>
        val filledForm =
          request.userAnswers.get(BondsListPage(srn)).fold(form)(form.fill)
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
                  pages.nonsipp.bonds.Paths.bondTransactions
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
      val indexes: List[Max5000] = request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

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
            errors => {
              bondsData(srn, indexes)
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
                        BondsJourneyStatus(srn),
                        if (!addAnother) SectionStatus.Completed else SectionStatus.InProgress
                      )
                      .set(BondsListPage(srn), addAnother)
                  )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (!addAnother) {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall = controllers.nonsipp.bonds.routes.BondsListController.onPageLoad(srn, page, mode)
                  )
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(BondsListPage(srn), mode, updatedUserAnswers)
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
          controllers.nonsipp.bonds.routes.BondsListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }

  private def bondsData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[BondsData]] =
    indexes
      .sortBy(x => x.value)
      .map { index =>
        for {
          nameOfBonds <- requiredPage(NameOfBondsPage(srn, index))
          acquisitionType <- requiredPage(WhyDoesSchemeHoldBondsPage(srn, index))
          costOfBonds <- requiredPage(CostOfBondsPage(srn, index))
        } yield BondsData(index, nameOfBonds, acquisitionType, costOfBonds)
      }
      .sequence
}

object BondsListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "bondsList.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[BondsData],
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[ListRow] =
    memberList.map {
      case BondsData(index, nameOfBonds, acquisition, costOfBonds) =>
        val acquisitionType = acquisition match {
          case SchemeHoldBond.Acquisition => "bondsList.acquisition.acquired"
          case SchemeHoldBond.Contribution => "bondsList.acquisition.contributed"
          case SchemeHoldBond.Transfer => "bondsList.acquisition.transferred"
        }
        val bondsMessage =
          Message("bondsList.row.withCost", nameOfBonds.show, acquisitionType, costOfBonds.displayAs)

        if (mode.isViewOnlyMode) {
          (mode, optYear, optCurrentVersion, optPreviousVersion) match {
            case (ViewOnlyMode, Some(year), Some(current), Some(previous)) =>
              ListRow.view(
                bondsMessage,
                //                controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                //                  .onPageLoadViewOnly(srn, index, year, current, previous)
                controllers.routes.UnauthorisedController.onPageLoad().url,
                Message("bondsList.row.change.hiddenText", bondsMessage)
              )
          }
        } else {

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
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[BondsData],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ListViewModel] = {
    val lengthOfData = data.length

    val (title, heading) = ((mode, lengthOfData) match {
      case (ViewOnlyMode, lengthOfData) if lengthOfData > 1 =>
        ("bondsList.view.title.plural", "bondsList.view.heading.plural")
      case (ViewOnlyMode, _) =>
        ("bondsList.view.title", "bondsList.view.heading")
      case (_, lengthOfData) if lengthOfData > 1 =>
        ("bondsList.title.plural", "bondsList.heading.plural")
      case _ =>
        ("bondsList.title", "bondsList.heading")
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
          controllers.nonsipp.bonds.routes.BondsListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.bonds.routes.BondsListController.onPageLoad(srn, _, mode)
      }
    )

    val conditionalInsetText: DisplayMessage = {
      if (data.size >= Constants.maxBondsTransactions) {
        ParagraphMessage("bondsList.inset")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = Some(ParagraphMessage("bondsList.description")),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows = rows(srn, mode, data, optYear, optCurrentVersion, optPreviousVersion),
        radioText = Message("bondsList.radios"),
        showRadios = data.size < Constants.maxBondsTransactions,
        showInsetWithRadios = !(data.length < Constants.maxBondsTransactions),
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
      optViewOnlyDetails = Option.when(mode.isViewOnlyMode) {
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if (optYear.nonEmpty && currentVersion > 1 && previousVersion > 0) =>
              Some(
                LinkMessage(
                  "bondsList.view.link",
                  controllers.nonsipp.bonds.routes.BondsListController
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
              controllers.nonsipp.bonds.routes.BondsListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.bonds.routes.BondsListController
                .onSubmit(srn, page, mode)
          }
        )
      }
    )
  }

  case class BondsData(
    index: Max5000,
    nameOfBonds: String,
    acquisitionType: SchemeHoldBond,
    costOfBonds: Money
  )

}
