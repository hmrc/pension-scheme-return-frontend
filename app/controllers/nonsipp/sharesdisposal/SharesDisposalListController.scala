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

import viewmodels.implicits._
import com.google.inject.Inject
import config.Constants
import cats.implicits.{toShow, toTraverseOps}
import pages.nonsipp.sharesdisposal._
import navigation.Navigator
import forms.RadioListFormProvider
import play.api.i18n.MessagesApi
import pages.nonsipp.shares._
import play.api.mvc._
import utils.ListUtils._
import config.RefinedTypes.Max5000.enumerable
import controllers.nonsipp.sharesdisposal.SharesDisposalListController._
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.ListRadiosView
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import java.time.LocalDate
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
        sharesDisposalData(srn, indexes).map { data =>
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
          sharesDisposalData(srn, indexes).map { sharesList =>
            BadRequest(view(errors, viewModel(srn, page, sharesList, request.userAnswers)))
          }.merge
        },
        answer =>
          Redirect(
            navigator.nextPage(
              SharesDisposalListPage(srn, answer),
              mode,
              request.userAnswers
            )
          )
      )
  }

  private def sharesDisposalData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[SharesDisposalData]] =
    indexes.map { index =>
      for {
        sharesType <- requiredPage(TypeOfSharesHeldPage(srn, index))
        companyName <- requiredPage(CompanyNameRelatedSharesPage(srn, index))
        acquisitionType <- requiredPage(WhyDoesSchemeHoldSharesPage(srn, index))
        acquisitionDate = req.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index))
      } yield SharesDisposalData(index, sharesType, companyName, acquisitionType, acquisitionDate)
    }.sequence
}

object SharesDisposalListController {
  def form(formProvider: RadioListFormProvider): Form[Max5000] =
    formProvider(
      "sharesDisposal.sharesDisposalList.radios.error.required"
    )

  private def buildRows(srn: Srn, shares: List[SharesDisposalData], userAnswers: UserAnswers): List[ListRadiosRow] =
    shares.flatMap { sharesDisposalData =>
      val completedDisposalsPerShareKeys = userAnswers
        .map(SharesDisposalProgress.all(srn, sharesDisposalData.index))
        .toList
        .filter(progress => progress._2.completed)
        .map(_._1)

      if (Constants.maxDisposalsPerShare == completedDisposalsPerShareKeys.size) {
        List[ListRadiosRow]().empty
      } else {

        val isShareShouldBeRemovedFromList = completedDisposalsPerShareKeys
          .map(_.toIntOption)
          .flatMap(_.traverse(index => refineV[Max50.Refined](index + 1).toOption))
          .flatMap(
            _.map(
              disposalIndex => userAnswers.get(HowManyDisposalSharesPage(srn, sharesDisposalData.index, disposalIndex))
            )
          )
          .exists(optValue => optValue.fold(false)(value => value == 0))

        if (isShareShouldBeRemovedFromList) {
          List[ListRadiosRow]().empty
        } else {
          List(
            ListRadiosRow(
              sharesDisposalData.index.value,
              buildMessage(sharesDisposalData)
            )
          )
        }
      }
    }

  private def buildMessage(sharesDisposalData: SharesDisposalData): Message =
    sharesDisposalData match {
      case SharesDisposalData(_, typeOfShares, companyName, methodOfAcquisition, dateOfAcquisition) =>
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
    sharesList: List[SharesDisposalData],
    userAnswers: UserAnswers
  ): FormPageViewModel[ListRadiosViewModel] = {

    val sortedSharesList = sharesList.sortBy(_.index.value)

    val rows = buildRows(srn, sortedSharesList, userAnswers)

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.sharesDisposalListSize,
      totalSize = rows.size,
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
        rows = rows,
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

  case class SharesDisposalData(
    index: Max5000,
    typeOfShares: TypeOfShares,
    companyName: String,
    acquisitionType: SchemeHoldShare,
    acquisitionDate: Option[LocalDate]
  )
}
