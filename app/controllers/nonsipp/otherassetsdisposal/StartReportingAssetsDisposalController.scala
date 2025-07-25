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
import config.Constants
import navigation.Navigator
import forms.RadioListFormProvider
import models.{Mode, Pagination, UserAnswers}
import pages.nonsipp.otherassetsheld._
import com.google.inject.Inject
import utils.ListUtils._
import config.RefinedTypes.Max5000.enumerable
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.ListRadiosView
import models.SchemeId.Srn
import cats.implicits.toTraverseOps
import controllers.nonsipp.otherassetsdisposal.StartReportingAssetsDisposalController._
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import play.api.i18n.MessagesApi
import viewmodels.LegendSize
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import javax.inject.Named

class StartReportingAssetsDisposalController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListRadiosView,
  formProvider: RadioListFormProvider
) extends PSRController {

  val form: Form[Max5000] = StartReportingAssetsDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val userAnswers = request.userAnswers
    val indexes: List[Max5000] =
      userAnswers.map(OtherAssetsCompleted.all()).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty) {
      assetsData(srn, indexes).map(assets => Ok(view(form, viewModel(srn, page, assets, userAnswers)))).merge
    } else {
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val userAnswers = request.userAnswers
    val assetsCompletedIndexes: List[Max5000] =
      userAnswers.map(OtherAssetsCompleted.all()).keys.toList.refine[Max5000.Refined]

    form
      .bindFromRequest()
      .fold(
        errors =>
          assetsData(srn, assetsCompletedIndexes).map { assets =>
            BadRequest(view(errors, viewModel(srn, page, assets, userAnswers)))
          }.merge,
        answer => {
          val inProgressUrl = request.userAnswers
            .map(OtherAssetsDisposalProgress.all(answer))
            .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
          inProgressUrl match {
            case Some(url) => Redirect(url)
            case _ =>
              Redirect(
                navigator.nextPage(
                  OtherAssetsDisposalListPage(srn, answer),
                  mode,
                  request.userAnswers
                )
              )
          }
        }
      )
  }

  private def assetsData(srn: Srn, indexes: List[Max5000])(implicit
    req: DataRequest[?]
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

  private def buildRows(srn: Srn, assets: List[AssetData], userAnswers: UserAnswers): List[ListRadiosRow] =
    assets.flatMap { assetData =>
      val completedDisposalsPerAssetKeys = userAnswers
        .map(OtherAssetsDisposalProgress.all(assetData.index))
        .toList
        .filter(progress => progress._2.completed)
        .map(_._1)

      if (Constants.maxDisposalPerOtherAsset == completedDisposalsPerAssetKeys.size) {
        List[ListRadiosRow]().empty
      } else {

        val isAssetShouldBeRemovedFromList = completedDisposalsPerAssetKeys
          .map(_.toIntOption)
          .flatMap(_.traverse(index => refineV[Max50.Refined](index + 1).toOption))
          .flatMap(
            _.map(disposalIndex => userAnswers.get(AnyPartAssetStillHeldPage(srn, assetData.index, disposalIndex)))
          )
          .exists(optValue => optValue.fold(false)(value => !value))

        if (isAssetShouldBeRemovedFromList) {
          List[ListRadiosRow]().empty
        } else {
          List(
            ListRadiosRow(
              assetData.index.value,
              assetData.nameOfAsset
            )
          )
        }
      }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    assets: List[AssetData],
    userAnswers: UserAnswers
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
        rows = buildRows(srn, sortedAssets, userAnswers),
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
