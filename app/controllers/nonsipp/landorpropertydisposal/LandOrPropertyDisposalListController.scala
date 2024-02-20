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

package controllers.nonsipp.landorpropertydisposal

import cats.implicits._
import com.google.inject.Inject
import config.Constants
import config.Constants.maxLandOrPropertyDisposals
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalListController._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Address, Mode, NormalMode, Pagination}
import navigation.Navigator
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPages, LandOrPropertyChosenAddressPage}
import pages.nonsipp.landorpropertydisposal._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.nonsipp.TaskListStatusUtils.getLandOrPropertyDisposalsTaskListStatusWithLink
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models._
import views.html.ListView

import javax.inject.Named

class LandOrPropertyDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form: Form[Boolean] = LandOrPropertyDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val (status, incompleteDisposalUrl) = getLandOrPropertyDisposalsTaskListStatusWithLink(request.userAnswers, srn)

      if (status == TaskListStatus.Completed) {
        getDisposals(srn).map { disposals =>
          val numberOfDisposal = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfAddresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn)).size
          val maxPossibleNumberOfDisposals = maxLandOrPropertyDisposals * numberOfAddresses
          getAddressesWithIndexes(srn, disposals)
            .map(
              indexes =>
                Ok(view(form, viewModel(srn, mode, page, indexes, numberOfDisposal, maxPossibleNumberOfDisposals)))
            )
            .merge
        }.merge
      } else if (status == TaskListStatus.InProgress) {
        Redirect(incompleteDisposalUrl)
      } else {
        Redirect(routes.LandOrPropertyDisposalController.onPageLoad(srn, NormalMode))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    getDisposals(srn).map { disposals =>
      val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfAddresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn)).size
      val maxPossibleNumberOfDisposals = maxLandOrPropertyDisposals * numberOfAddresses
      if (numberOfDisposals == maxPossibleNumberOfDisposals) {
        Redirect(
          navigator.nextPage(LandOrPropertyDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              getAddressesWithIndexes(srn, disposals)
                .map(
                  indexes =>
                    BadRequest(
                      view(errors, viewModel(srn, mode, page, indexes, numberOfDisposals, maxPossibleNumberOfDisposals))
                    )
                )
                .merge,
            answer =>
              Redirect(navigator.nextPage(LandOrPropertyDisposalListPage(srn, answer), mode, request.userAnswers))
          )
      }
    }.merge
  }

  private def getDisposals(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(LandPropertyDisposalCompletedPages(srn))
      .filter(_._2.nonEmpty)
      .map {
        case (key, sectionCompleted) =>
          val maybeLandOrPropertyIndex: Either[Result, Max5000] =
            refineStringIndex[Max5000.Refined](key).getOrRecoverJourney

          val maybeDisposalIndexes: Either[Result, List[Max50]] =
            sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)

          for {
            landOrPropertyIndex <- maybeLandOrPropertyIndex
            disposalIndexes <- maybeDisposalIndexes
          } yield (landOrPropertyIndex, disposalIndexes)
      }
      .toList
      .sequence
      .map(_.toMap)

  private def getAddressesWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(
    implicit request: DataRequest[_]
  ): Either[Result, List[((Max5000, List[Max50]), Address)]] =
    disposals
      .map {
        case indexes @ (landOrPropertyIndex, _) =>
          request.userAnswers
            .get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex))
            .getOrRecoverJourney
            .map(address => (indexes, address))
      }
      .toList
      .sequence

}

object LandOrPropertyDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "landOrPropertyDisposalList.radios.error.required"
    )

  private def rows(srn: Srn, mode: Mode, addressesWithIndexes: List[((Max5000, List[Max50]), Address)]): List[ListRow] =
    addressesWithIndexes.flatMap {
      case ((index, disposalIndexes), address) =>
        disposalIndexes.map { x =>
          ListRow(
            Message("landOrPropertyDisposalList.row", address.addressLine1),
            changeUrl = routes.LandPropertyDisposalCYAController
              .onPageLoad(srn, index, x, mode)
              .url,
            changeHiddenText = Message("landOrPropertyDisposalList.row.change.hidden"),
            removeUrl = routes.RemoveLandPropertyDisposalController.onPageLoad(srn, index, x, NormalMode).url,
            removeHiddenText = Message("landOrPropertyDisposalList.row.remove.hidden")
          )
        }
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    addressesWithIndexes: List[((Max5000, List[Max50]), Address)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = if (numberOfDisposals == 1) {
      ("landOrPropertyDisposalList.title", "landOrPropertyDisposalList.heading")
    } else {
      ("landOrPropertyDisposalList.title.plural", "landOrPropertyDisposalList.heading.plural")
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertyDisposalsSize,
      numberOfDisposals,
      routes.LandOrPropertyDisposalListController.onPageLoad(srn, _)
    )

    FormPageViewModel(
      title = Message(title, numberOfDisposals),
      heading = Message(heading, numberOfDisposals),
      description = Option.when(numberOfDisposals < maxPossibleNumberOfDisposals)(
        ParagraphMessage("landOrPropertyDisposalList.description")
      ),
      page = ListViewModel(
        inset = "landOrPropertyDisposalList.inset",
        rows(srn, mode, addressesWithIndexes),
        Message("landOrPropertyDisposalList.radios"),
        showRadios = numberOfDisposals < maxPossibleNumberOfDisposals,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "landOrPropertyDisposalList.pagination.label",
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
      onSubmit = routes.LandOrPropertyDisposalListController.onSubmit(srn, page)
    )
  }
}
