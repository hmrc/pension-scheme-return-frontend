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

import pages.nonsipp.bonds.{BondsCompleted, NameOfBondsPage}
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import cats.implicits._
import config.Constants.{maxBondsTransactions, maxDisposalPerBond}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import models._
import models.HowDisposed.HowDisposed
import com.google.inject.Inject
import utils.nonsipp.TaskListStatusUtils.getBondsDisposalsTaskListStatusWithLink
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.nonsipp.bondsdisposal.ReportBondsDisposalListController._
import forms.YesNoPageFormProvider
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal.{
  BondsDisposalCompletedPages,
  HowWereBondsDisposedOfPage,
  ReportBondsDisposalListPage
}
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import javax.inject.Named

class ReportBondsDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form: Form[Boolean] = ReportBondsDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val (status, incompleteDisposalUrl) = getBondsDisposalsTaskListStatusWithLink(request.userAnswers, srn, mode)

      if (status == TaskListStatus.Completed) {
        getDisposals(srn).map { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfBondsItems = request.userAnswers.map(BondsCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalPerBond * numberOfBondsItems
          getBondsDisposalsWithIndexes(srn, disposals)
            .map(
              bondsDisposalsWithIndexes =>
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
                      request.userAnswers
                    )
                  )
                )
            )
            .merge
        }.merge
      } else if (status == TaskListStatus.InProgress) {
        Redirect(incompleteDisposalUrl)
      } else {
        Redirect(routes.BondsDisposalController.onPageLoad(srn, NormalMode))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    getDisposals(srn).map { disposals =>
      val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfBondsItems = request.userAnswers.map(BondsCompleted.all(srn)).size
      val maxPossibleNumberOfDisposals = maxDisposalPerBond * numberOfBondsItems
      if (numberOfDisposals == maxPossibleNumberOfDisposals) {
        Redirect(
          navigator.nextPage(ReportBondsDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              getBondsDisposalsWithIndexes(srn, disposals)
                .map(
                  indexes =>
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
                          request.userAnswers
                        )
                      )
                    )
                )
                .merge,
            answer => Redirect(navigator.nextPage(ReportBondsDisposalListPage(srn, answer), mode, request.userAnswers))
          )
      }
    }.merge
  }

  private def getDisposals(srn: Srn)(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(BondsDisposalCompletedPages(srn))
      .filter(_._2.nonEmpty)
      .map {
        case (key, sectionCompleted) =>
          val maybeBondsIndex: Either[Result, Max5000] =
            refineStringIndex[Max5000.Refined](key).getOrRecoverJourney

          val maybeDisposalIndexes: Either[Result, List[Max50]] =
            sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)

          for {
            bondIndex <- maybeBondsIndex
            disposalIndexes <- maybeDisposalIndexes
          } yield (bondIndex, disposalIndexes)
      }
      .toList
      .sequence
      .map(_.toMap)

  private def getBondsDisposalsWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(
    implicit request: DataRequest[_]
  ): Either[Result, List[((Max5000, List[Max50]), SectionCompleted)]] =
    disposals
      .map {
        case indexes @ (index, _) =>
          request.userAnswers
            .get(BondsCompleted(srn, index))
            .getOrRecoverJourney
            .map(bondsDisposal => (indexes, bondsDisposal))
      }
      .toList
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
    userAnswers: UserAnswers
  ): List[ListRow] =
    bondsDisposalsWithIndexes.flatMap {
      case ((bondIndex, disposalIndexes), bondsDisposal) =>
        disposalIndexes.map { disposalIndex =>
          val bondsDisposalData = BondsDisposalData(
            bondIndex,
            disposalIndex,
            userAnswers.get(NameOfBondsPage(srn, bondIndex)).get,
            userAnswers.get(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex)).get
          )

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
    userAnswers: UserAnswers
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = if (numberOfDisposals == 1) {
      ("bondsDisposal.reportBondsDisposalList.title", "bondsDisposal.reportBondsDisposalList.heading")
    } else {
      (
        "bondsDisposal.reportBondsDisposalList.title.plural",
        "bondsDisposal.reportBondsDisposalList.heading.plural"
      )
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.reportedSharesDisposalListSize,
      numberOfDisposals,
      routes.ReportBondsDisposalListController.onPageLoad(srn, _)
    )

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxBondsTransactions) {
        Message("bondsDisposal.reportBondsDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals) {
        ParagraphMessage("bondsDisposal.reportBondsDisposalList.inset.allBondsDisposed.paragraph1") ++
          ParagraphMessage("bondsDisposal.reportBondsDisposalList.inset.allBondsDisposed.paragraph2")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      title = Message(title, numberOfDisposals),
      heading = Message(heading, numberOfDisposals),
      description = Option.when(
        !((numberOfDisposals >= maxBondsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals))
      )(
        ParagraphMessage("bondsDisposal.reportBondsDisposalList.description")
      ),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, mode, bondsDisposalsWithIndexes, userAnswers),
        Message("bondsDisposal.reportBondsDisposalList.radios"),
        showRadios =
          !((numberOfDisposals >= maxBondsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals)),
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
      onSubmit = routes.ReportBondsDisposalListController.onSubmit(srn, page)
    )
  }

  case class BondsDisposalData(
    bondIndex: Max5000,
    disposalIndex: Max50,
    nameOfBonds: String,
    disposalMethod: HowDisposed
  )
}
