/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import play.api.i18n.MessagesApi
import controllers.nonsipp.bonds.BondsListController._
import config.Constants
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
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
  view: TwoColumnsTripleAction,
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
                  psrSubmissionService.submitPsrDetails(srn, updatedUserAnswers)
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
    indexes.map { index =>
      for {
        nameOfBonds <- requiredPage(NameOfBondsPage(srn, index))
        acquisitionType <- requiredPage(WhyDoesSchemeHoldBondsPage(srn, index))
        costOfBonds <- requiredPage(CostOfBondsPage(srn, index))
      } yield BondsData(index, nameOfBonds, acquisitionType, costOfBonds)
    }.sequence
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
  ): List[List[TableElem]] =
    memberList.map {
      case BondsData(index, nameOfBonds, acquisition, costOfBonds) =>
        val acquisitionType = acquisition match {
          case SchemeHoldBond.Acquisition => "bondsList.acquisition.acquired"
          case SchemeHoldBond.Contribution => "bondsList.acquisition.contributed"
          case SchemeHoldBond.Transfer => "bondsList.acquisition.transferred"
        }
        val bondsMessage =
          Message("bondsList.row.withCost", nameOfBonds.show, acquisitionType, costOfBonds.displayAs)

        List(
          TableElem(bondsMessage),
          TableElem(
            LinkMessage(
              Message("site.change"),
              controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                .onPageLoad(srn, index, CheckMode)
                .url
            )
          ),
          TableElem(
            LinkMessage(
              Message("site.remove"),
              controllers.nonsipp.bonds.routes.RemoveBondsController
                .onPageLoad(srn, index, NormalMode)
                .url
            )
          )
        )
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[BondsData]
  ): FormPageViewModel[ActionTableViewModel] = {
    val title = if (data.size > 1) "bondsList.title.plural" else "bondsList.title"
    val heading = if (data.size > 1) "bondsList.heading.plural" else "bondsList.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.pageSize,
      totalSize = data.size,
      call = controllers.nonsipp.bonds.routes.BondsListController.onPageLoad(srn, _, mode)
    )

    FormPageViewModel(
      title = Message(title, data.size),
      heading = Message(heading, data.size),
      description = Some(ParagraphMessage("bondsList.description")),
      page = ActionTableViewModel(
        inset = ParagraphMessage("bondsList.inset"),
        head = None,
        rows = rows(srn, mode, data),
        radioText = Message("bondsList.radios"),
        showRadios = data.size < Constants.maxBondsTransactions,
        showInsetWithRadios = !(data.length < Constants.maxBondsTransactions),
        showInset = data.size >= Constants.maxBondsTransactions,
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
