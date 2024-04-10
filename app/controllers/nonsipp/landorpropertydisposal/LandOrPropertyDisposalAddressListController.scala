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

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.{Max5000, _}
import controllers.PSRController
import config.Constants
import cats.implicits._
import pages.nonsipp.landorpropertydisposal.{
  LandOrPropertyDisposalAddressListPage,
  LandOrPropertyStillHeldPage,
  LandPropertyDisposalCompletedPages
}
import config.Refined.Max5000._
import navigation.Navigator
import forms.RadioListFormProvider
import models._
import play.api.i18n.MessagesApi
import play.api.data.Form
import views.html.ListRadiosView
import models.SchemeId.Srn
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalAddressListController._
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPages
import controllers.actions._
import eu.timepit.refined.refineV
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._

import scala.collection.immutable.SortedMap

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
      val userAnswers = request.userAnswers
      val addresses = userAnswers.map(LandOrPropertyAddressLookupPages(srn))
      withIndexedAddress(addresses) { sortedAddresses =>
        Ok(view(form, viewModel(srn, page, sortedAddresses, userAnswers)))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val userAnswers = request.userAnswers
    val addresses = userAnswers.map(LandOrPropertyAddressLookupPages(srn))
    form
      .bindFromRequest()
      .fold(
        errors =>
          withIndexedAddress(addresses)(
            sortedAddresses => BadRequest(view(errors, viewModel(srn, page, sortedAddresses, userAnswers)))
          ),
        answer =>
          LandOrPropertyDisposalAddressListController
            .getDisposal(srn, answer, userAnswers, isNextDisposal = true)
            .getOrRecoverJourney(
              nextDisposal =>
                Redirect(
                  navigator.nextPage(
                    LandOrPropertyDisposalAddressListPage(srn, answer, nextDisposal),
                    mode,
                    request.userAnswers
                  )
                )
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

  private def getDisposal(
    srn: Srn,
    addressChoice: Max5000,
    userAnswers: UserAnswers,
    isNextDisposal: Boolean
  ): Option[Max50] =
    userAnswers.get(LandPropertyDisposalCompletedPages(srn)) match {
      case None => refineV[Max50.Refined](1).toOption
      case Some(completedDisposals) =>
        /**
         * Indexes of completed disposals sorted in ascending order.
         * We -1 from the address choice as the refined indexes is 1-based (e.g. 1 to 5000)
         * while we are trying to fetch a completed disposal from a Map which is 0-based.
         * We then +1 when we re-refine the index
         */
        val completedDisposalsForAddress =
          completedDisposals
            .get((addressChoice.value - 1).toString)
            .map(_.keys.toList)
            .flatMap(_.traverse(_.toIntOption))
            .flatMap(_.traverse(index => refineV[Max50.Refined](index + 1).toOption))
            .toList
            .flatten
            .sortBy(_.value)

        completedDisposalsForAddress.lastOption match {
          case None => refineV[Max50.Refined](1).toOption
          case Some(lastCompletedDisposalForAddress) =>
            if (isNextDisposal) {
              refineV[Max50.Refined](lastCompletedDisposalForAddress.value + 1).toOption
            } else {
              refineV[Max50.Refined](lastCompletedDisposalForAddress.value).toOption
            }
        }
    }

  private def buildRows(srn: Srn, addresses: Map[Int, Address], userAnswers: UserAnswers): List[ListRadiosRow] =
    addresses.flatMap {
      case (index, address) =>
        refineV[Max5000.Refined](index + 1).fold(
          _ => Nil,
          nextIndex => {
            val disposalIndex = getDisposal(srn, nextIndex, userAnswers, isNextDisposal = false).get
            val isDisposed: Option[Boolean] = {
              userAnswers.get(LandOrPropertyStillHeldPage(srn, nextIndex, disposalIndex))
            }
            isDisposed match {
              case Some(value) =>
                if (value) {
                  List(
                    ListRadiosRow(
                      nextIndex.value,
                      address.addressLine1
                    )
                  )
                } else {
                  val empty: List[ListRadiosRow] = List()
                  empty
                }
              case _ =>
                List(
                  ListRadiosRow(
                    nextIndex.value,
                    address.addressLine1
                  )
                )
            }
          }
        )
    }.toList

  def viewModel(
    srn: Srn,
    page: Int,
    addresses: Map[Int, Address],
    userAnswers: UserAnswers
  ): FormPageViewModel[ListRadiosViewModel] = {
    val rows = buildRows(srn, addresses, userAnswers)

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
}
