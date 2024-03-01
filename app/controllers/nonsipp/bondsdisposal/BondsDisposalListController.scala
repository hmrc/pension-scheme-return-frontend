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

package controllers.nonsipp.bondsdisposal

import cats.implicits.toTraverseOps
import com.google.inject.Inject
import config.Constants
import config.Refined.{Max50, Max5000}
import config.Refined.Max5000.enumerable
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.bondsdisposal.BondsDisposalListController._
import eu.timepit.refined.{refineMV, refineV}
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, Money, Pagination, SchemeHoldBond, UserAnswers}
import navigation.Navigator
import pages.nonsipp.bondsdisposal.{BondsDisposalCompletedPages, BondsDisposalListPage}
import pages.nonsipp.unregulatedorconnectedbonds.{
  BondsCompleted,
  CostOfBondsPage,
  NameOfBondsPage,
  WhyDoesSchemeHoldBondsPage
}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.ListUtils._
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.LegendSize
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel, PaginatedViewModel}
import views.html.ListRadiosView

import javax.inject.Named

class BondsDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5000] = BondsDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val indexes = request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.nonEmpty) {
        bondsData(srn, indexes).map { data =>
          Ok(view(form, viewModel(srn, page, data, mode)))
        }.merge
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val indexes: List[Max5000] =
      request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    form
      .bindFromRequest()
      .fold(
        errors => {
          bondsData(srn, indexes).map { bondsList =>
            BadRequest(view(errors, viewModel(srn, page, bondsList, mode)))
          }.merge
        },
        answer =>
          BondsDisposalListController
            .getDisposal(srn, answer, request.userAnswers, isNextDisposal = true)
            .getOrRecoverJourney(
              nextDisposal =>
                Redirect(
                  navigator.nextPage(
                    BondsDisposalListPage(srn, answer, nextDisposal),
                    mode,
                    request.userAnswers
                  )
                )
            )
      )
  }

  private def bondsData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[BondsDisposalData]] =
    indexes.map { index =>
      for {
        nameOfBonds <- requiredPage(NameOfBondsPage(srn, index))
        heldBondsType <- requiredPage(WhyDoesSchemeHoldBondsPage(srn, index))
        bondsValue <- requiredPage(CostOfBondsPage(srn, index))
      } yield BondsDisposalData(index, nameOfBonds, heldBondsType, bondsValue)
    }.sequence
}

object BondsDisposalListController {
  def form(formProvider: RadioListFormProvider): Form[Max5000] =
    formProvider("bondsDisposalList.error.required")

  private def getDisposal(
    srn: Srn,
    bondIndex: Max5000,
    userAnswers: UserAnswers,
    isNextDisposal: Boolean
  ): Option[Max50] =
    userAnswers.get(BondsDisposalCompletedPages(srn)) match {
      case None => Some(refineMV[Max50.Refined](1))
      case Some(completedDisposals) =>
        /**
         * Indexes of completed disposals sorted in ascending order.
         * We -1 from the bond index as the refined indexes is 1-based (e.g. 1 to 5000)
         * while we are trying to fetch a completed disposal from a Map which is 0-based.
         * We then +1 when we re-refine the index
         */
        val completedDisposalsForBonds: List[Max50] =
          completedDisposals
            .get((bondIndex.value - 1).toString)
            .map(_.keys.toList)
            .flatMap(_.traverse(_.toIntOption))
            .flatMap(_.traverse(index => refineV[Max50.Refined](index + 1).toOption))
            .toList
            .flatten
            .sortBy(_.value)

        completedDisposalsForBonds.lastOption match {
          case None => Some(refineMV[Max50.Refined](1))
          case Some(lastCompletedDisposalForBonds) =>
            if (isNextDisposal) {
              refineV[Max50.Refined](lastCompletedDisposalForBonds.value + 1).toOption
            } else {
              Some(lastCompletedDisposalForBonds)
            }
        }
    }

  private def buildRows(bondsData: List[BondsDisposalData]): List[ListRadiosRow] =
    bondsData.flatMap { bonds =>
      val heldBonds = bonds.heldBondsType match {
        case SchemeHoldBond.Acquisition => "bondsDisposalList.acquired"
        case SchemeHoldBond.Contribution => "bondsDisposalList.contributed"
        case SchemeHoldBond.Transfer => "bondsDisposalList.transferred"
      }
      List(
        ListRadiosRow(
          bonds.index.value,
          Message("bondsDisposalList.row", bonds.nameOfBonds, heldBonds, bonds.bondsValue.displayAs)
        )
      )
    }

  def viewModel(
    srn: Srn,
    page: Int,
    bondsList: List[BondsDisposalData],
    mode: Mode
  ): FormPageViewModel[ListRadiosViewModel] = {

    val sortedBondsList = bondsList.sortBy(_.index.value)

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.bondsDisposalListSize,
      totalSize = sortedBondsList.size,
      page => routes.BondsDisposalListController.onPageLoad(srn, page, mode)
    )

    FormPageViewModel(
      title = "bondsDisposalList.title",
      heading = "bondsDisposalList.heading",
      description = Some(
        ParagraphMessage("bondsDisposalList.paragraph1") ++
          ParagraphMessage("bondsDisposalList.paragraph2")
      ),
      page = ListRadiosViewModel(
        legend = Some("bondsDisposalList.legend"),
        legendSize = Some(LegendSize.Medium),
        rows = buildRows(sortedBondsList),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "bondsDisposalList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      refresh = None,
      buttonText = Message("site.saveAndContinue"),
      details = None,
      onSubmit = routes.BondsDisposalListController.onSubmit(srn, page, mode)
    )
  }

  case class BondsDisposalData(
    index: Max5000,
    nameOfBonds: String,
    heldBondsType: SchemeHoldBond,
    bondsValue: Money
  )
}
