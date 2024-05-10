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

package controllers.nonsipp.sharesdisposal

import services.{PsrSubmissionService, SaveService}
import controllers.nonsipp.sharesdisposal.ReportedSharesDisposalListController._
import viewmodels.implicits._
import com.google.inject.Inject
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import config.Constants
import cats.implicits._
import config.Constants.{maxDisposalsPerShare, maxSharesTransactions}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal._
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesCompleted, TypeOfSharesHeldPage}
import play.api.mvc._
import views.html.ListView
import models.TypeOfShares._
import models.SchemeId.Srn
import navigation.Navigator
import models.HowSharesDisposed._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class ReportedSharesDisposalListController @Inject()(
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

  val form: Form[Boolean] = ReportedSharesDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      getCompletedDisposals(srn).map { completedDisposals =>
        if (completedDisposals.values.exists(_.nonEmpty)) {
          Ok(view(form, viewModel(srn, page, completedDisposals, request.userAnswers)))
        } else {
          Redirect(routes.SharesDisposalController.onPageLoad(srn, NormalMode))
        }
      }.merge
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getCompletedDisposals(srn)
        .map { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfSharesItems = request.userAnswers.map(SharesCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems

          if (numberOfDisposals == maxPossibleNumberOfDisposals) {
            Redirect(
              navigator.nextPage(ReportedSharesDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors => BadRequest(view(errors, viewModel(srn, page, disposals, request.userAnswers))).pure[Future],
                reportAnotherDisposal =>
                  for {
                    updatedUserAnswers <- request.userAnswers
                      .setWhen(!reportAnotherDisposal)(SharesDisposalCompleted(srn), SectionCompleted)
                      .mapK[Future]
                    _ <- saveService.save(updatedUserAnswers)
                    submissionResult <- if (!reportAnotherDisposal) {
                      psrSubmissionService.submitPsrDetailsWithUA(
                        srn,
                        updatedUserAnswers,
                        fallbackCall = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                          .onPageLoad(srn, page)
                      )
                    } else {
                      Future.successful(Some(()))
                    }
                  } yield submissionResult.getOrRecoverJourney(
                    _ =>
                      Redirect(
                        navigator.nextPage(
                          ReportedSharesDisposalListPage(srn, reportAnotherDisposal),
                          mode,
                          request.userAnswers
                        )
                      )
                  )
              )
          }
        }
        .leftMap(_.pure[Future])
        .merge
  }

  private def getCompletedDisposals(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(SharesDisposalProgress.all(srn))
      .map {
        case (key, secondaryMap) =>
          key -> secondaryMap.filter { case (_, status) => status.completed }
      }
      .toList
      .traverse {
        case (key, sectionCompleted) =>
          for {
            sharesIndex <- refineStringIndex[Max5000.Refined](key).getOrRecoverJourney
            disposalIndexes <- sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)
          } yield (sharesIndex, disposalIndexes)
      }
      .map(_.toMap)
}

object ReportedSharesDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "sharesDisposal.reportedSharesDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers
  ): List[ListRow] =
    disposals
      .flatMap {
        case (shareIndex, disposalIndexes) =>
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
              removeUrl =
                routes.RemoveShareDisposalController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode).url,
              removeHiddenText = buildMessage(
                "sharesDisposal.reportedSharesDisposalList.row.remove.hidden",
                sharesDisposalData
              )
            )
          }
      }
      .toList
      .sortBy(_.changeUrl)

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
    page: Int,
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers
  ): FormPageViewModel[ListViewModel] = {

    val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
    val numberOfSharesItems = userAnswers.map(SharesCompleted.all(srn)).size
    val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems

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
        rows(srn, disposals, userAnswers),
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
