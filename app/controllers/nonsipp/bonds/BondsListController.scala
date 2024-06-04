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
import controllers.nonsipp.bonds.BondsListController._
import cats.implicits.{catsSyntaxApplicativeId, toShow, toTraverseOps}
import _root_.config.Constants
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import views.html.ListView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
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
    extends PSRController {

  val form: Form[Boolean] = BondsListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val indexes: List[Max5000] = request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.nonEmpty) {
        bondsData(srn, indexes).map { data =>
          val filledForm =
            request.userAnswers.get(BondsListPage(srn)).fold(form)(form.fill)
          Ok(view(filledForm, viewModel(srn, page, mode, data)))
        }.merge
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
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
                  BadRequest(view(errors, viewModel(srn, page, mode, data)))
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
    memberList: List[BondsData]
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

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[BondsData]
  ): FormPageViewModel[ListViewModel] = {
    val title = if (data.size > 1) "bondsList.title.plural" else "bondsList.title"
    val heading = if (data.size > 1) "bondsList.heading.plural" else "bondsList.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.pageSize,
      totalSize = data.size,
      call = controllers.nonsipp.bonds.routes.BondsListController.onPageLoad(srn, _, mode)
    )

    val conditionalInsetText: DisplayMessage = {
      if (data.size >= Constants.maxBondsTransactions) {
        ParagraphMessage("bondsList.inset")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      title = Message(title, data.size),
      heading = Message(heading, data.size),
      description = Some(ParagraphMessage("bondsList.description")),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows = rows(srn, mode, data),
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
      onSubmit = controllers.nonsipp.bonds.routes.BondsListController.onSubmit(srn, page, mode)
    )
  }

  case class BondsData(
    index: Max5000,
    nameOfBonds: String,
    acquisitionType: SchemeHoldBond,
    costOfBonds: Money
  )

}
