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

import cats.implicits.{toShow, toTraverseOps}
import com.google.inject.Inject
import config.Constants
import config.Refined.Max5000.enumerable
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.shares.SharesListController.SharesData
import controllers.nonsipp.sharesdisposal.SharesDisposalListController._
import eu.timepit.refined.{refineMV, refineV}
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, Pagination, SchemeHoldShare, TypeOfShares, UserAnswers}
import navigation.Navigator
import pages.nonsipp.shares.{
  CompanyNameRelatedSharesPage,
  SharesCompleted,
  TypeOfSharesHeldPage,
  WhenDidSchemeAcquireSharesPage,
  WhyDoesSchemeHoldSharesPage
}
import pages.nonsipp.sharesdisposal.{HowManySharesPage, SharesDisposalCompletedPages, SharesDisposalListPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.DateTimeUtils.localDateShow
import utils.ListUtils._
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel, PaginatedViewModel}
import views.html.ListRadiosView

import javax.inject.Named

class SharesDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5000] = SharesDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val indexes = request.userAnswers.map(SharesCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.nonEmpty) {
        sharesData(srn, indexes).map { data =>
          Ok(view(form, viewModel(srn, page, data, request.userAnswers)))
        }.merge
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val indexes: List[Max5000] = request.userAnswers.map(SharesCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    form
      .bindFromRequest()
      .fold(
        errors => {
          sharesData(srn, indexes).map { sharesList =>
            BadRequest(view(errors, viewModel(srn, page, sharesList, request.userAnswers)))
          }.merge
        },
        answer =>
          SharesDisposalListController
            .getDisposal(srn, answer, request.userAnswers, isNextDisposal = true)
            .getOrRecoverJourney(
              nextDisposal =>
                Redirect(
                  navigator.nextPage(
                    SharesDisposalListPage(srn, answer, nextDisposal),
                    mode,
                    request.userAnswers
                  )
                )
            )
      )
  }

  private def sharesData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[SharesData]] =
    indexes.map { index =>
      for {
        sharesType <- requiredPage(TypeOfSharesHeldPage(srn, index))
        companyName <- requiredPage(CompanyNameRelatedSharesPage(srn, index))
        acquisitionType <- requiredPage(WhyDoesSchemeHoldSharesPage(srn, index))
        acquisitionDate = req.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index))
      } yield SharesData(index, sharesType, companyName, acquisitionType, acquisitionDate)
    }.sequence
}

object SharesDisposalListController {
  def form(formProvider: RadioListFormProvider): Form[Max5000] =
    formProvider(
      "sharesDisposal.sharesDisposalList.radios.error.required"
    )

  private def getDisposal(
    srn: Srn,
    shareIndex: Max5000,
    userAnswers: UserAnswers,
    isNextDisposal: Boolean
  ): Option[Max50] =
    userAnswers.get(SharesDisposalCompletedPages(srn)) match {
      case None => Some(refineMV[Max50.Refined](1))
      case Some(completedDisposals) =>
        /**
         * Indexes of completed disposals sorted in ascending order.
         * We -1 from the share index as the refined indexes is 1-based (e.g. 1 to 5000)
         * while we are trying to fetch a completed disposal from a Map which is 0-based.
         * We then +1 when we re-refine the index
         */
        val completedDisposalsForShares: List[Max50] =
          completedDisposals
            .get((shareIndex.value - 1).toString)
            .map(_.keys.toList)
            .flatMap(_.traverse(_.toIntOption))
            .flatMap(_.traverse(index => refineV[Max50.Refined](index + 1).toOption))
            .toList
            .flatten
            .sortBy(_.value)

        completedDisposalsForShares.lastOption match {
          case None => Some(refineMV[Max50.Refined](1))
          case Some(lastCompletedDisposalForShares) =>
            if (isNextDisposal) {
              refineV[Max50.Refined](lastCompletedDisposalForShares.value + 1).toOption
            } else {
              Some(lastCompletedDisposalForShares)
            }
        }
    }

  private def buildRows(srn: Srn, shares: List[SharesData], userAnswers: UserAnswers): List[ListRadiosRow] =
    shares.flatMap { sharesData =>
      val disposalIndex = getDisposal(srn, sharesData.index, userAnswers, isNextDisposal = false).get
      val totalSharesNowHeld: Option[Int] = userAnswers.get(HowManySharesPage(srn, sharesData.index, disposalIndex))
      totalSharesNowHeld match {
        case Some(sharesRemaining) =>
          if (sharesRemaining > 0) {
            List(
              ListRadiosRow(
                sharesData.index.value,
                buildMessage(sharesData)
              )
            )
          } else {
            Nil
          }
        case _ =>
          List(
            ListRadiosRow(
              sharesData.index.value,
              buildMessage(sharesData)
            )
          )
      }
    }

  private def buildMessage(sharesData: SharesData): Message =
    sharesData match {
      case SharesData(_, typeOfShares, companyName, methodOfAcquisition, dateOfAcquisition) =>
        val sharesType = typeOfShares match {
          case TypeOfShares.SponsoringEmployer => "sharesDisposal.sharesDisposalList.typeOfShares.sponsoringEmployer"
          case TypeOfShares.Unquoted => "sharesDisposal.sharesDisposalList.typeOfShares.unquoted"
          case TypeOfShares.ConnectedParty => "sharesDisposal.sharesDisposalList.typeOfShares.connectedParty"
        }
        val acquisitionType = methodOfAcquisition match {
          case SchemeHoldShare.Acquisition => "sharesDisposal.sharesDisposalList.methodOfAcquisition.acquired"
          case SchemeHoldShare.Contribution => "sharesDisposal.sharesDisposalList.methodOfAcquisition.contributed"
          case SchemeHoldShare.Transfer => "sharesDisposal.sharesDisposalList.methodOfAcquisition.transferred"
        }
        val sharesMessage = dateOfAcquisition match {
          case Some(acquisitionDate) =>
            Message(
              "sharesDisposal.sharesDisposalList.row.withDate",
              sharesType,
              companyName,
              acquisitionType,
              acquisitionDate.show
            )
          case None => Message("sharesDisposal.sharesDisposalList.row", sharesType, companyName, acquisitionType)
        }
        sharesMessage
    }

  def viewModel(
    srn: Srn,
    page: Int,
    sharesList: List[SharesData],
    userAnswers: UserAnswers
  ): FormPageViewModel[ListRadiosViewModel] = {

    val sortedSharesList = sharesList.sortBy(_.index.value)

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.sharesDisposalListSize,
      totalSize = sortedSharesList.size,
      page => routes.SharesDisposalListController.onPageLoad(srn, page)
    )

    FormPageViewModel(
      title = "sharesDisposal.sharesDisposalList.title",
      heading = "sharesDisposal.sharesDisposalList.heading",
      description = Some(
        ParagraphMessage("sharesDisposal.sharesDisposalList.paragraph1") ++
          ParagraphMessage("sharesDisposal.sharesDisposalList.paragraph2")
      ),
      page = ListRadiosViewModel(
        legend = Some("sharesDisposal.sharesDisposalList.legend"),
        rows = buildRows(srn, sortedSharesList, userAnswers),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "sharesDisposal.sharesDisposalList.pagination.label",
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
      onSubmit = routes.SharesDisposalListController.onSubmit(srn, page)
    )
  }
}
