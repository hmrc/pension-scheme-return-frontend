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

package controllers.nonsipp.otherassetsdisposal

import pages.nonsipp.otherassetsdisposal._
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils._
import config.Refined.Max5000
import controllers.PSRController
import config.Constants
import controllers.actions.IdentifyAndRequireData
import config.Refined.Max5000.enumerable
import navigation.Navigator
import forms.RadioListFormProvider
import models.{Mode, Pagination}
import pages.nonsipp.otherassetsheld._
import com.google.inject.Inject
import views.html.ListRadiosView
import models.SchemeId.Srn
import cats.implicits.toTraverseOps
import controllers.nonsipp.otherassetsdisposal.StartReportingAssetsDisposalController._
import play.api.i18n.MessagesApi
import viewmodels.LegendSize
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import javax.inject.Named

class StartReportingAssetsDisposalController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5000] = StartReportingAssetsDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val indexes = request.userAnswers.map(OtherAssetsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.nonEmpty) {
        assetsData(srn, indexes).map(assets => Ok(view(form, viewModel(srn, page, assets)))).merge
      } else {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val indexes: List[Max5000] =
      request.userAnswers.map(OtherAssetsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    form
      .bindFromRequest()
      .fold(
        errors => {
          assetsData(srn, indexes).map { assets =>
            BadRequest(view(errors, viewModel(srn, page, assets)))
          }.merge
        },
        answer =>
          Redirect(
            navigator.nextPage(
              OtherAssetsDisposalListPage(srn, answer),
              mode,
              request.userAnswers
            )
          )
      )
  }

  private def assetsData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[AssetData]] =
    indexes.traverse { index =>
      for {
        nameOfAsset <- requiredPage(WhatIsOtherAssetPage(srn, index))
      } yield AssetData(index, nameOfAsset)
    }
}

object StartReportingAssetsDisposalController {
  def form(formProvider: RadioListFormProvider): Form[Max5000] =
    formProvider("otherAssetsDisposal.startReportingAssetsDisposal.error.required")

  private def buildRows(assets: List[AssetData]): List[ListRadiosRow] =
    assets.flatMap { asset =>
      List(
        ListRadiosRow(
          asset.index.value,
          Message(asset.nameOfAsset)
        )
      )
    }

  def viewModel(
    srn: Srn,
    page: Int,
    assets: List[AssetData]
  ): FormPageViewModel[ListRadiosViewModel] = {

    val sortedAssets = assets.sortBy(_.index.value)

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.reportingAssetsDisposalDisposalListSize,
      totalSize = sortedAssets.size,
      page => routes.StartReportingAssetsDisposalController.onPageLoad(srn, page)
    )

    FormPageViewModel(
      title = "otherAssetsDisposal.startReportingAssetsDisposal.title",
      heading = "otherAssetsDisposal.startReportingAssetsDisposal.heading",
      description = Some(
        ParagraphMessage("otherAssetsDisposal.startReportingAssetsDisposal.paragraph1") ++
          ParagraphMessage("otherAssetsDisposal.startReportingAssetsDisposal.paragraph2")
      ),
      page = ListRadiosViewModel(
        legend = Some("otherAssetsDisposal.startReportingAssetsDisposal.legend"),
        legendSize = Some(LegendSize.Medium),
        rows = buildRows(sortedAssets),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "otherAssetsDisposal.pagination.label",
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
      onSubmit = routes.StartReportingAssetsDisposalController.onSubmit(srn, page)
    )
  }

  case class AssetData(
    index: Max5000,
    nameOfAsset: String
  )
}
