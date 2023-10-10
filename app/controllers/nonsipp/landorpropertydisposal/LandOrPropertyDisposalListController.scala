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
import models.CheckOrChange.Change
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Address, Mode, NormalMode, Pagination}
import navigation.Navigator
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPage
import pages.nonsipp.landorpropertydisposal._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel, PaginatedViewModel}
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

  val form = LandOrPropertyDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      getDisposals(srn)
        .map(
          disposals =>
            if (disposals.isEmpty) {
              Redirect(routes.LandOrPropertyDisposalAddressListController.onPageLoad(srn, page = 1))
            } else {
              getAddressesWithIndexes(srn, disposals)
                .map(indexes => Ok(view(form, viewModel(srn, page, indexes))))
                .merge
            }
        )
        .merge
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    getDisposals(srn).map { disposals =>
      if (disposals.size == maxLandOrPropertyDisposals) {
        Redirect(
          navigator.nextPage(LandOrPropertyDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              getAddressesWithIndexes(srn, disposals)
                .map(indexes => BadRequest(view(errors, viewModel(srn, page, indexes))))
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
      .map(LandOrPropertyStillHeldPages(srn))
      .map {
        case (key, stillHeldMap) =>
          val maybeLandOrPropertyIndex: Either[Result, Max5000] =
            refineStringIndex[Max5000.Refined](key).getOrRecoverJourney

          val maybeDisposalIndexes: Either[Result, List[Max50]] =
            stillHeldMap.keys.toList
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
            .get(LandOrPropertyAddressLookupPage(srn, landOrPropertyIndex))
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

  private def rows(srn: Srn, addressesWithIndexes: List[((Max5000, List[Max50]), Address)]): List[ListRow] =
    addressesWithIndexes.flatMap {
      case ((index, disposalIndexes), address) =>
        disposalIndexes.map { x =>
          ListRow(
            Message("landOrPropertyDisposalList.row", address.addressLine1),
            changeUrl = routes.LandPropertyDisposalCYAController
              .onPageLoad(srn, index, x, Change)
              .url,
            changeHiddenText = Message("landOrPropertyDisposalList.row.change.hidden"),
            removeUrl = routes.RemoveLandPropertyDisposalController.onPageLoad(srn, index, x, NormalMode).url,
            removeHiddenText = Message("landOrPropertyDisposalList.row.remove.hidden")
          )
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    addressesWithIndexes: List[((Max5000, List[Max50]), Address)]
  ): FormPageViewModel[ListViewModel] = {

    val disposalAmount = addressesWithIndexes.map { case ((_, disposalIndexes), _) => disposalIndexes.size }.sum

    val title =
      if (disposalAmount == 1) "landOrPropertyDisposalList.title"
      else "landOrPropertyDisposalList.title.plural"
    val heading =
      if (disposalAmount == 1) "landOrPropertyDisposalList.heading"
      else "landOrPropertyDisposalList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertyDisposalsSize,
      disposalAmount,
      routes.LandOrPropertyDisposalListController.onPageLoad(srn, _)
    )

    FormPageViewModel(
      title = Message(title, disposalAmount),
      heading = Message(heading, disposalAmount),
      description = Some(ParagraphMessage("landOrPropertyDisposalList.description")),
      page = ListViewModel(
        inset = "landOrPropertyDisposalList.inset",
        rows(srn, addressesWithIndexes),
        Message("landOrPropertyDisposalList.radios"),
        showRadios = disposalAmount < maxLandOrPropertyDisposals,
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
