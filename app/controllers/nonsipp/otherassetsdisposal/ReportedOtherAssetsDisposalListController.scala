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

import pages.nonsipp.otherassetsdisposal.{
  HowWasAssetDisposedOfPage,
  OtherAssetsDisposalCompletedPages,
  ReportedOtherAssetsDisposalListPage
}
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import cats.implicits._
import config.Constants.{maxDisposalPerOtherAsset, maxOtherAssetsTransactions}
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.otherassetsdisposal.ReportedOtherAssetsDisposalListController._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import models.HowDisposed._
import com.google.inject.Inject
import utils.nonsipp.TaskListStatusUtils.getOtherAssetsDisposalTaskListStatusAndLink
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import javax.inject.Named

class ReportedOtherAssetsDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form: Form[Boolean] = ReportedOtherAssetsDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val (status, incompleteDisposalUrl) = getOtherAssetsDisposalTaskListStatusAndLink(request.userAnswers, srn)

      if (status == TaskListStatus.Completed) {
        getDisposals(srn).map { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfOtherAssetsItems = request.userAnswers.map(OtherAssetsCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems
          getOtherAssetsDisposalsWithIndexes(srn, disposals)
            .map(
              otherAssetsDisposalsWithIndexes =>
                Ok(
                  view(
                    form,
                    viewModel(
                      srn,
                      page,
                      otherAssetsDisposalsWithIndexes,
                      numberOfDisposals,
                      maxPossibleNumberOfDisposals,
                      request.userAnswers
                    )
                  )
                )
            )
            .merge
        }.merge
      } else if (status == TaskListStatus.InProgress) {
        Redirect(incompleteDisposalUrl)
      } else {
        Redirect(routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    getDisposals(srn).map { disposals =>
      val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfOtherAssetsItems = request.userAnswers.map(OtherAssetsCompleted.all(srn)).size
      val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems
      if (numberOfDisposals == maxPossibleNumberOfDisposals) {
        Redirect(
          navigator.nextPage(ReportedOtherAssetsDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              getOtherAssetsDisposalsWithIndexes(srn, disposals)
                .map(
                  indexes =>
                    BadRequest(
                      view(
                        errors,
                        viewModel(
                          srn,
                          page,
                          indexes,
                          numberOfDisposals,
                          maxPossibleNumberOfDisposals,
                          request.userAnswers
                        )
                      )
                    )
                )
                .merge,
            answer =>
              Redirect(navigator.nextPage(ReportedOtherAssetsDisposalListPage(srn, answer), mode, request.userAnswers))
          )
      }
    }.merge
  }

  private def getDisposals(srn: Srn)(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(OtherAssetsDisposalCompletedPages(srn))
      .filter(_._2.nonEmpty)
      .map {
        case (key, sectionCompleted) =>
          val maybeOtherAssetsIndex: Either[Result, Max5000] =
            refineStringIndex[Max5000.Refined](key).getOrRecoverJourney

          val maybeDisposalIndexes: Either[Result, List[Max50]] =
            sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)

          for {
            otherAssetsIndex <- maybeOtherAssetsIndex
            disposalIndexes <- maybeDisposalIndexes
          } yield (otherAssetsIndex, disposalIndexes)
      }
      .toList
      .sequence
      .map(_.toMap)

  private def getOtherAssetsDisposalsWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(
    implicit request: DataRequest[_]
  ): Either[Result, List[((Max5000, List[Max50]), SectionCompleted)]] =
    disposals
      .map {
        case indexes @ (otherAssetsIndex, _) =>
          request.userAnswers
            .get(OtherAssetsCompleted(srn, otherAssetsIndex))
            .getOrRecoverJourney
            .map(otherAssetsDisposal => (indexes, otherAssetsDisposal))
      }
      .toList
      .sequence
}

object ReportedOtherAssetsDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "assetDisposal.reportedOtherAssetsDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    otherAssetsDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    userAnswers: UserAnswers
  ): List[ListRow] =
    otherAssetsDisposalsWithIndexes.flatMap {
      case ((otherAssetsIndex, disposalIndexes), _) =>
        disposalIndexes.map { disposalIndex =>
          val otherAssetsDisposalData = OtherAssetsDisposalData(
            otherAssetsIndex,
            disposalIndex,
            userAnswers.get(WhatIsOtherAssetPage(srn, otherAssetsIndex)).get,
            userAnswers.get(HowWasAssetDisposedOfPage(srn, otherAssetsIndex, disposalIndex)).get
          )

          ListRow(
            buildMessage("assetDisposal.reportedOtherAssetsDisposalList.row", otherAssetsDisposalData),
            changeUrl = routes.AssetDisposalCYAController
              .onPageLoad(srn, otherAssetsIndex, disposalIndex, CheckMode)
              .url,
            changeHiddenText = buildMessage(
              "assetDisposal.reportedOtherAssetsDisposalList.row.change.hidden",
              otherAssetsDisposalData
            ),
            removeUrl = routes.RemoveAssetDisposalController
              .onPageLoad(srn, otherAssetsIndex, disposalIndex)
              .url,
            removeHiddenText = buildMessage(
              "assetDisposal.reportedOtherAssetsDisposalList.row.remove.hidden",
              otherAssetsDisposalData
            )
          )
        }
    }

  private def buildMessage(messageString: String, otherAssetsDisposalData: OtherAssetsDisposalData): Message =
    otherAssetsDisposalData match {
      case OtherAssetsDisposalData(_, _, companyName, typeOfDisposal) =>
        val disposalType = typeOfDisposal match {
          case Sold => "assetDisposal.reportedOtherAssetsDisposalList.methodOfDisposal.sold"
          case Transferred => "assetDisposal.reportedOtherAssetsDisposalList.methodOfDisposal.transferred"
          case Other(_) => "assetDisposal.reportedOtherAssetsDisposalList.methodOfDisposal.other"
        }
        Message(messageString, companyName, disposalType)
    }

  def viewModel(
    srn: Srn,
    page: Int,
    otherAssetsDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int,
    userAnswers: UserAnswers
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = if (numberOfDisposals == 1) {
      ("assetDisposal.reportedOtherAssetsDisposalList.title", "assetDisposal.reportedOtherAssetsDisposalList.heading")
    } else {
      (
        "assetDisposal.reportedOtherAssetsDisposalList.title.plural",
        "assetDisposal.reportedOtherAssetsDisposalList.heading.plural"
      )
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.reportedOtherAssetsDisposalListSize,
      numberOfDisposals,
      routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, _)
    )

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxOtherAssetsTransactions) {
        Message("assetDisposal.reportedOtherAssetsDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals) {
        ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.inset.allOtherAssetsDisposed.paragraph1") ++
          ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.inset.allOtherAssetsDisposed.paragraph2")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      title = Message(title, numberOfDisposals),
      heading = Message(heading, numberOfDisposals),
      description = Option.when(
        !((numberOfDisposals >= maxOtherAssetsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals))
      )(
        ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.description")
      ),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, otherAssetsDisposalsWithIndexes, userAnswers),
        Message("assetDisposal.reportedOtherAssetsDisposalList.radios"),
        showRadios =
          !((numberOfDisposals >= maxOtherAssetsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals)),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "assetDisposal.reportedOtherAssetsDisposalList.pagination.label",
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
      onSubmit = routes.ReportedOtherAssetsDisposalListController.onSubmit(srn, page)
    )
  }

  case class OtherAssetsDisposalData(
    otherAssetsIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    disposalMethod: HowDisposed
  )
}
