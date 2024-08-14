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
import play.api.Logging
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
    extends PSRController
    with Logging {

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
    onPageLoadCommon(srn, page, mode, Some(viewOnlyViewModel))(implicitly)
  }

  def onPageLoadCommon(srn: Srn, page: Int, mode: Mode, viewOnlyViewModel: Option[ViewOnlyViewModel] = None)(
    implicit request: DataRequest[AnyContent]
  ): Result = {
    val indexes: List[Max5000] = request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty || mode.isViewOnlyMode) {
      bondsData(srn, indexes).map { data =>
        val filledForm =
          request.userAnswers.get(BondsListPage(srn)).fold(form)(form.fill)
        Ok(
          view(
            filledForm,
            BondsListController.viewModel(
              srn,
              page,
              mode,
              data,
              request.schemeDetails.schemeName,
              viewOnlyViewModel
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
                  BadRequest(view(errors, viewModel(srn, page, mode, data, request.schemeDetails.schemeName)))
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
  ): Either[Result, List[BondsListController.BondsData]] =
    indexes
      .sortBy(x => x.value)
      .map { index =>
        for {
          nameOfBonds <- requiredPage(NameOfBondsPage(srn, index))
          acquisitionType <- requiredPage(WhyDoesSchemeHoldBondsPage(srn, index))
          costOfBonds <- requiredPage(CostOfBondsPage(srn, index))
        } yield BondsListController.BondsData(index, nameOfBonds, acquisitionType, costOfBonds)
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
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    (memberList, mode) match {
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
        list.map {
          case BondsData(index, nameOfBonds, acquisition, costOfBonds) =>
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
                  Message("bondsList.row.change.hiddenText", bondsMessage)
                )
              case _ =>
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
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[BondsData],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None
  ): FormPageViewModel[ListViewModel] = {
    val lengthOfData = data.length

    val (title, heading) = ((mode, lengthOfData) match {
      case (ViewOnlyMode, numberOfLoans) if numberOfLoans == 0 =>
        ("bondsList.view.title", "bondsList.view.heading.none")
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
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          controllers.nonsipp.bonds.routes.BondsListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
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
        rows = rows(srn, mode, data, viewOnlyViewModel, schemeName),
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
