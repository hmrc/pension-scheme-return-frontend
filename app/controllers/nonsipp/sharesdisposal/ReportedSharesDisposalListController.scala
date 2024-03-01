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

package controllers.nonsipp.sharesdisposal

import cats.implicits._
import com.google.inject.Inject
import config.Constants
import config.Constants.{maxDisposalsPerShare, maxSharesTransactions}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.sharesdisposal.ReportedSharesDisposalListController._
import forms.YesNoPageFormProvider
import models.HowSharesDisposed._
import models.SchemeId.Srn
import models.TypeOfShares._
import models.requests.DataRequest
import models.{CheckMode, Mode, NormalMode, Pagination, TypeOfShares, UserAnswers}
import navigation.Navigator
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesCompleted, TypeOfSharesHeldPage}
import pages.nonsipp.sharesdisposal.{
  HowWereSharesDisposedPage,
  ReportedSharesDisposalListPage,
  SharesDisposalCompletedPages
}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.nonsipp.TaskListStatusUtils.getSharesDisposalsTaskListStatusWithLink
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models._
import views.html.ListView

import javax.inject.Named

class ReportedSharesDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form: Form[Boolean] = ReportedSharesDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val (status, incompleteDisposalUrl) = getSharesDisposalsTaskListStatusWithLink(request.userAnswers, srn)

      if (status == TaskListStatus.Completed) {
        getDisposals(srn).map { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfSharesItems = request.userAnswers.map(SharesCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems
          getSharesDisposalsWithIndexes(srn, disposals)
            .map(
              sharesDisposalsWithIndexes =>
                Ok(
                  view(
                    form,
                    viewModel(
                      srn,
                      mode,
                      page,
                      sharesDisposalsWithIndexes,
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
        Redirect(routes.SharesDisposalController.onPageLoad(srn, NormalMode))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    getDisposals(srn).map { disposals =>
      val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfSharesItems = request.userAnswers.map(SharesCompleted.all(srn)).size
      val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems
      if (numberOfDisposals == maxPossibleNumberOfDisposals) {
        Redirect(
          navigator.nextPage(ReportedSharesDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              getSharesDisposalsWithIndexes(srn, disposals)
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
            answer =>
              Redirect(navigator.nextPage(ReportedSharesDisposalListPage(srn, answer), mode, request.userAnswers))
          )
      }
    }.merge
  }

  private def getDisposals(srn: Srn)(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(SharesDisposalCompletedPages(srn))
      .filter(_._2.nonEmpty)
      .map {
        case (key, sectionCompleted) =>
          val maybeSharesIndex: Either[Result, Max5000] =
            refineStringIndex[Max5000.Refined](key).getOrRecoverJourney

          val maybeDisposalIndexes: Either[Result, List[Max50]] =
            sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)

          for {
            sharesIndex <- maybeSharesIndex
            disposalIndexes <- maybeDisposalIndexes
          } yield (sharesIndex, disposalIndexes)
      }
      .toList
      .sequence
      .map(_.toMap)

  private def getSharesDisposalsWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(
    implicit request: DataRequest[_]
  ): Either[Result, List[((Max5000, List[Max50]), SectionCompleted)]] =
    disposals
      .map {
        case indexes @ (shareIndex, _) =>
          request.userAnswers
            .get(SharesCompleted(srn, shareIndex))
            .getOrRecoverJourney
            .map(sharesDisposal => (indexes, sharesDisposal))
      }
      .toList
      .sequence
}

object ReportedSharesDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "sharesDisposal.reportedSharesDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    sharesDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    userAnswers: UserAnswers
  ): List[ListRow] =
    sharesDisposalsWithIndexes.flatMap {
      case ((shareIndex, disposalIndexes), sharesDisposal) =>
        disposalIndexes.map { disposalIndex =>
          val sharesDisposalData = SharesDisposalData(
            shareIndex,
            disposalIndex,
            userAnswers.get(TypeOfSharesHeldPage(srn, shareIndex)).get,
            userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).get,
            userAnswers.get(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex)).get
          )

          ListRow(
            buildMessage("sharesDisposal.reportedSharesDisposalList.row", sharesDisposalData),
            changeUrl = routes.SharesDisposalCYAController
              .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
              .url,
            changeHiddenText = buildMessage(
              "sharesDisposal.reportedSharesDisposalList.row.change.hidden",
              sharesDisposalData
            ),
            removeUrl = routes.RemoveShareDisposalController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode).url,
            removeHiddenText = buildMessage(
              "sharesDisposal.reportedSharesDisposalList.row.remove.hidden",
              sharesDisposalData
            )
          )
        }
    }

  private def buildMessage(messageString: String, sharesDisposalData: SharesDisposalData): Message =
    sharesDisposalData match {
      case SharesDisposalData(_, _, typeOfShares, companyName, typeOfDisposal) =>
        val sharesType = typeOfShares match {
          case SponsoringEmployer => "sharesDisposal.reportedSharesDisposalList.typeOfShares.sponsoringEmployer"
          case Unquoted => "sharesDisposal.reportedSharesDisposalList.typeOfShares.unquoted"
          case ConnectedParty => "sharesDisposal.reportedSharesDisposalList.typeOfShares.connectedParty"
        }
        val disposalType = typeOfDisposal match {
          case Sold => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.sold"
          case Redeemed => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.redeemed"
          case Transferred => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.transferred"
          case Other(_) => "sharesDisposal.reportedSharesDisposalList.methodOfDisposal.other"
        }
        Message(messageString, sharesType, companyName, disposalType)
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    sharesDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int,
    userAnswers: UserAnswers
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = if (numberOfDisposals == 1) {
      ("sharesDisposal.reportedSharesDisposalList.title", "sharesDisposal.reportedSharesDisposalList.heading")
    } else {
      (
        "sharesDisposal.reportedSharesDisposalList.title.plural",
        "sharesDisposal.reportedSharesDisposalList.heading.plural"
      )
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.reportedSharesDisposalListSize,
      numberOfDisposals,
      routes.ReportedSharesDisposalListController.onPageLoad(srn, _)
    )

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxSharesTransactions) {
        Message("sharesDisposal.reportedSharesDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals) {
        ParagraphMessage("sharesDisposal.reportedSharesDisposalList.inset.allSharesDisposed.paragraph1") ++
          ParagraphMessage("sharesDisposal.reportedSharesDisposalList.inset.allSharesDisposed.paragraph2")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      title = Message(title, numberOfDisposals),
      heading = Message(heading, numberOfDisposals),
      description = Option.when(
        !((numberOfDisposals >= maxSharesTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals))
      )(
        ParagraphMessage("sharesDisposal.reportedSharesDisposalList.description")
      ),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, mode, sharesDisposalsWithIndexes, userAnswers),
        Message("sharesDisposal.reportedSharesDisposalList.radios"),
        showRadios =
          !((numberOfDisposals >= maxSharesTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals)),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "sharesDisposal.reportedSharesDisposalList.pagination.label",
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
      onSubmit = routes.ReportedSharesDisposalListController.onSubmit(srn, page)
    )
  }

  case class SharesDisposalData(
    shareIndex: Max5000,
    disposalIndex: Max50,
    sharesType: TypeOfShares,
    companyName: String,
    disposalMethod: HowSharesDisposed
  )
}
