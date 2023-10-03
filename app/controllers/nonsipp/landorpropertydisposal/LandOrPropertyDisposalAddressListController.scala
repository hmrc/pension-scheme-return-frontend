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
import config.Refined.Max5000
import config.Refined.Max5000._
import controllers.actions._
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalAddressListController._
import eu.timepit.refined.refineV
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.{Address, Mode, Pagination}
import navigation.Navigator
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPages
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalAddressListPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, ListRadiosRow, ListRadiosViewModel, PaginatedViewModel}
import views.html.ListRadiosView

import javax.inject.Named
import scala.collection.immutable.SortedMap

class LandOrPropertyDisposalAddressListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends FrontendBaseController
    with I18nSupport {

  val form = LandOrPropertyDisposalAddressListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val addresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn))
      withIndexedAddress(addresses) { sortedAddresses =>
        Ok(view(form, viewModel(srn, page, sortedAddresses)))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val addresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn))
    form
      .bindFromRequest()
      .fold(
        errors =>
          withIndexedAddress(addresses) { sortedAddresses =>
            BadRequest(view(errors, viewModel(srn, page, sortedAddresses)))
          },
        answer =>
          Redirect(
            navigator.nextPage(LandOrPropertyDisposalAddressListPage(srn, answer), mode, request.userAnswers)
          )
      )
  }

  private def withIndexedAddress(addresses: Map[String, Address])(
    f: Map[Int, Address] => Result
  ): Result = {
    val maybeIndexedAddresses: Option[List[(Int, Address)]] = addresses.toList
      .traverse { case (key, value) => key.toIntOption.map(_ -> value) }

    maybeIndexedAddresses match {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(indexedAddresses) =>
        val sortedMap = SortedMap.from(indexedAddresses)
        f(sortedMap)
    }
  }
}

object LandOrPropertyDisposalAddressListController {
  def form(formProvider: RadioListFormProvider): Form[Max5000] =
    formProvider(
      "landOrPropertyDisposalAddressList.radios.error.required"
    )

  private def buildRows(addresses: Map[Int, Address]): List[ListRadiosRow] =
    addresses.flatMap {
      case (index, address) =>
        refineV[Max5000.Refined](index + 1).fold(
          _ => Nil,
          index =>
            List(
              ListRadiosRow(
                index.value,
                address.addressLine1
              )
            )
        )
    }.toList

  def viewModel(
    srn: Srn,
    page: Int,
    addresses: Map[Int, Address]
  ): FormPageViewModel[ListRadiosViewModel] = {
    val rows = buildRows(addresses)

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertiesSize,
      totalSize = rows.size,
      page => routes.LandOrPropertyDisposalListController.onPageLoad(srn, page)
    )

    FormPageViewModel(
      title = "landOrPropertyDisposalAddressList.title",
      heading = "landOrPropertyDisposalAddressList.heading",
      description = Some(
        ParagraphMessage("landOrPropertyDisposalAddressList.paragraph1") ++
          ParagraphMessage("landOrPropertyDisposalAddressList.paragraph2")
      ),
      page = ListRadiosViewModel(
        legend = "landOrPropertyDisposalAddressList.legend",
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
}
