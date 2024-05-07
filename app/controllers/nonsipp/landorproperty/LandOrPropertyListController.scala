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

package controllers.nonsipp.landorproperty

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import config.Refined.Max5000
import controllers.PSRController
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPages, LandOrPropertyListPage}
import config.Constants.maxLandOrProperties
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import play.api.i18n.MessagesApi
import play.api.data.Form
import utils.nonsipp.TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._

import javax.inject.Named

class LandOrPropertyListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form: Form[Boolean] = LandOrPropertyListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val userAnswers = request.userAnswers
      val (status, incompleteLandOrPropertyUrl) = getLandOrPropertyTaskListStatusAndLink(userAnswers, srn)
      if (status == TaskListStatus.Completed) {
        val addresses = userAnswers.map(LandOrPropertyAddressLookupPages(srn))
        val viewModel = LandOrPropertyListController.viewModel(srn, page, mode, addresses)
        Ok(view(form, viewModel))
      } else if (status == TaskListStatus.InProgress) {
        Redirect(incompleteLandOrPropertyUrl)
      } else {
        Redirect(controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, mode))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val addresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn))

    if (addresses.size == maxLandOrProperties) {
      Redirect(navigator.nextPage(LandOrPropertyListPage(srn, addLandOrProperty = false), mode, request.userAnswers))
    } else {
      val viewModel = LandOrPropertyListController.viewModel(srn, page, mode, addresses)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer =>
            Redirect(
              navigator.nextPage(LandOrPropertyListPage(srn, addLandOrProperty = answer), mode, request.userAnswers)
            )
        )
    }
  }
}

object LandOrPropertyListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "landOrPropertyList.radios.error.required"
    )

  private def rows(srn: Srn, mode: Mode, addresses: Map[String, Address]): List[ListRow] =
    addresses
      .flatMap {
        case (index, address) =>
          refineV[Max5000.Refined](index.toInt + 1).fold(
            _ => Nil,
            index =>
              List(
                index -> ListRow(
                  address.addressLine1,
                  changeUrl = routes.LandOrPropertyCYAController.onPageLoad(srn, index, CheckMode).url,
                  changeHiddenText = Message("landOrPropertyList.row.change.hiddenText", address.addressLine1),
                  routes.RemovePropertyController.onPageLoad(srn, index, mode).url,
                  Message("landOrPropertyList.row.remove.hiddenText")
                )
              )
          )
      }
      .toList
      .sortBy { case (index, _) => index.value }
      .map { case (_, listRow) => listRow }

  def viewModel(srn: Srn, page: Int, mode: Mode, addresses: Map[String, Address]): FormPageViewModel[ListViewModel] = {

    val title = if (addresses.size == 1) "landOrPropertyList.title" else "landOrPropertyList.title.plural"
    val heading = if (addresses.size == 1) "landOrPropertyList.heading" else "landOrPropertyList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertiesSize,
      addresses.size,
      routes.LandOrPropertyListController.onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      Message(title, addresses.size),
      Message(heading, addresses.size),
      ParagraphMessage("landOrPropertyList.paragraph"),
      ListViewModel(
        inset = "landOrPropertyList.inset",
        rows(srn, mode, addresses),
        Message("landOrPropertyList.radios"),
        showRadios = addresses.size < Constants.maxLandOrProperties,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "landOrPropertyList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      routes.LandOrPropertyListController.onSubmit(srn, page, mode)
    )
  }
}
