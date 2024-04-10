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

import pages.nonsipp.bonds._
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.ListUtils._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import config.Constants
import cats.implicits.toTraverseOps
import config.Constants.maxDisposalPerBond
import config.Refined.Max5000.enumerable
import controllers.nonsipp.bondsdisposal.BondsDisposalListController._
import navigation.Navigator
import forms.RadioListFormProvider
import models._
import views.html.ListRadiosView
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal.{BondsDisposalCompleted, BondsDisposalListPage, BondsStillHeldPage}
import viewmodels.LegendSize
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

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
      val completedBonds = request.userAnswers.map(BondsCompleted.all(srn))

      if (completedBonds.nonEmpty) {
        bondsData(srn, completedBonds.keys.toList.refine[Max5000.Refined]).map { data =>
          Ok(view(form, viewModel(srn, page, data, mode, request.userAnswers)))
        }.merge
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    form
      .bindFromRequest()
      .fold(
        errors => {

          val completedBondIndexes: List[Max5000] =
            request.userAnswers.map(BondsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]
          bondsData(srn, completedBondIndexes).map { bondsList =>
            BadRequest(view(errors, viewModel(srn, page, bondsList, mode, request.userAnswers)))

          }.merge
        },
        answer =>
          Redirect(
            navigator.nextPage(
              BondsDisposalListPage(srn, answer),
              mode,
              request.userAnswers
            )
          )
      )
  }

  private def bondsData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[BondsData]] =
    indexes.map { index =>
      for {
        nameOfBonds <- requiredPage(NameOfBondsPage(srn, index))
        heldBondsType <- requiredPage(WhyDoesSchemeHoldBondsPage(srn, index))
        bondsValue <- requiredPage(CostOfBondsPage(srn, index))
      } yield BondsData(index, nameOfBonds, heldBondsType, bondsValue)
    }.sequence
}

object BondsDisposalListController {
  def form(formProvider: RadioListFormProvider): Form[Max5000] =
    formProvider("bondsDisposalList.error.required")

  private def buildRows(srn: Srn, bondsList: List[BondsData], userAnswers: UserAnswers): List[ListRadiosRow] =
    bondsList.flatMap { bondsData =>
      val completedDisposalsPerBondKeys = userAnswers
        .map(BondsDisposalCompleted.all(srn, bondsData.index))
        .keys

      if (maxDisposalPerBond == completedDisposalsPerBondKeys.size) {
        List[ListRadiosRow]().empty
      } else {

        val isBondShouldBeRemovedFromList = completedDisposalsPerBondKeys.toList
          .map(_.toIntOption)
          .flatMap(_.traverse(index => refineV[Max50.Refined](index + 1).toOption))
          .flatMap(_.map(disposalIndex => userAnswers.get(BondsStillHeldPage(srn, bondsData.index, disposalIndex))))
          .exists(optValue => optValue.fold(false)(value => value == 0))

        if (isBondShouldBeRemovedFromList) {
          List[ListRadiosRow]().empty
        } else {
          List(
            ListRadiosRow(
              bondsData.index.value,
              buildMessage(bondsData)
            )
          )
        }
      }
    }

  private def buildMessage(bondsData: BondsData): Message = {
    val heldBondsMessage = bondsData.heldBondsType match {
      case SchemeHoldBond.Acquisition => "bondsDisposalList.acquired"
      case SchemeHoldBond.Contribution => "bondsDisposalList.contributed"
      case SchemeHoldBond.Transfer => "bondsDisposalList.transferred"
    }
    Message("bondsDisposalList.row", bondsData.nameOfBonds, heldBondsMessage, bondsData.bondsValue.displayAs)
  }

  def viewModel(
    srn: Srn,
    page: Int,
    bondsList: List[BondsData],
    mode: Mode,
    userAnswers: UserAnswers
  ): FormPageViewModel[ListRadiosViewModel] = {

    val sortedBondsList = bondsList.sortBy(_.index.value)

    val rows = buildRows(srn, sortedBondsList, userAnswers)

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.bondsDisposalListSize,
      totalSize = rows.size,
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
        rows = rows,
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

  case class BondsData(
    index: Max5000,
    nameOfBonds: String,
    heldBondsType: SchemeHoldBond,
    bondsValue: Money
  )
}
