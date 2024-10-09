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

package controllers.nonsipp.landorpropertydisposal

import _root_.config.RefinedTypes.Max5000.enumerable
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import controllers.PSRController
import cats.implicits._
import _root_.config.Constants.maxLandOrPropertyDisposals
import navigation.Navigator
import forms.RadioListFormProvider
import models._
import play.api.i18n.MessagesApi
import _root_.config.RefinedTypes.{Max50, Max5000}
import com.google.inject.Inject
import views.html.ListRadiosView
import models.SchemeId.Srn
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalAddressListController._
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandOrPropertyCompleted}
import _root_.config.Constants
import pages.nonsipp.landorpropertydisposal._
import controllers.actions._
import eu.timepit.refined.refineV
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import javax.inject.Named

class LandOrPropertyDisposalAddressListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5000] = LandOrPropertyDisposalAddressListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val completedLandOrProperties = request.userAnswers.map(LandOrPropertyCompleted.all(srn))

      if (completedLandOrProperties.nonEmpty) {
        landOrPropertyData(srn, completedLandOrProperties.keys.toList.refine[Max5000.Refined]).map { data =>
          Ok(view(form, viewModel(srn, page, data, request.userAnswers)))
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
          val completedLandOrPropertyIndexes: List[Max5000] =
            request.userAnswers.map(LandOrPropertyCompleted.all(srn)).keys.toList.refine[Max5000.Refined]
          landOrPropertyData(srn, completedLandOrPropertyIndexes).map { data =>
            BadRequest(view(errors, viewModel(srn, page, data, request.userAnswers)))

          }.merge
        },
        answer =>
          Redirect(
            navigator.nextPage(
              LandOrPropertyDisposalAddressListPage(srn, answer),
              mode,
              request.userAnswers
            )
          )
      )
  }

  private def landOrPropertyData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[LandOrPropertyData]] =
    indexes.map { index =>
      for {
        address <- requiredPage(LandOrPropertyChosenAddressPage(srn, index))
      } yield LandOrPropertyData(index, address)
    }.sequence
}

object LandOrPropertyDisposalAddressListController {
  def form(formProvider: RadioListFormProvider): Form[Max5000] =
    formProvider(
      "landOrPropertyDisposalAddressList.radios.error.required"
    )

  private def buildRows(
    srn: Srn,
    landOrPropertyList: List[LandOrPropertyData],
    userAnswers: UserAnswers
  ): List[ListRadiosRow] =
    landOrPropertyList.flatMap { landOrPropertyData =>
      val completedDisposalsPerBondKeys = userAnswers
        .map(LandPropertyDisposalCompleted.all(srn, landOrPropertyData.index))
        .keys

      if (maxLandOrPropertyDisposals == completedDisposalsPerBondKeys.size) {
        List[ListRadiosRow]().empty
      } else {

        val isLandOrPropertyShouldBeRemovedFromList = completedDisposalsPerBondKeys.toList
          .map(_.toIntOption)
          .flatMap(_.traverse(index => refineV[Max50.Refined](index + 1).toOption))
          .flatMap(
            _.map(
              disposalIndex =>
                userAnswers.get(LandOrPropertyStillHeldPage(srn, landOrPropertyData.index, disposalIndex))
            )
          )
          .exists(optValue => optValue.fold(false)(value => !value))

        if (isLandOrPropertyShouldBeRemovedFromList) {
          List[ListRadiosRow]().empty
        } else {
          List(
            ListRadiosRow(
              landOrPropertyData.index.value,
              landOrPropertyData.address.addressLine1
            )
          )
        }
      }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    addresses: List[LandOrPropertyData],
    userAnswers: UserAnswers
  ): FormPageViewModel[ListRadiosViewModel] = {
    val rows = buildRows(srn, addresses.sortBy(_.index.value), userAnswers)

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertiesSize,
      totalSize = rows.size,
      page => routes.LandOrPropertyDisposalAddressListController.onPageLoad(srn, page)
    )

    FormPageViewModel(
      title = "landOrPropertyDisposalAddressList.title",
      heading = "landOrPropertyDisposalAddressList.heading",
      description = Some(
        ParagraphMessage("landOrPropertyDisposalAddressList.paragraph1") ++
          ParagraphMessage("landOrPropertyDisposalAddressList.paragraph2")
      ),
      page = ListRadiosViewModel(
        legend = Some("landOrPropertyDisposalAddressList.legend"),
        rows = rows,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "landOrPropertyDisposalAddressList.pagination.label",
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
      routes.LandOrPropertyDisposalAddressListController.onSubmit(srn, page)
    )
  }

  case class LandOrPropertyData(
    index: Max5000,
    address: Address
  )
}
