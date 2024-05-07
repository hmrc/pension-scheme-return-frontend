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

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.otherassetsdisposal._
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import config.Constants
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
import views.html.ListView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class ReportedOtherAssetsDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = ReportedOtherAssetsDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      getCompletedDisposals(srn).map { completedDisposals =>
        if (completedDisposals.values.exists(_.nonEmpty)) {
          Ok(view(form, viewModel(srn, page, completedDisposals, request.userAnswers)))
        } else {
          Redirect(routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode))
        }
      }.merge
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getCompletedDisposals(srn)
        .map { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfOtherAssetsItems = request.userAnswers.map(OtherAssetsCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems

          if (numberOfDisposals == maxPossibleNumberOfDisposals) {
            Redirect(
              navigator
                .nextPage(ReportedOtherAssetsDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors => BadRequest(view(errors, viewModel(srn, page, disposals, request.userAnswers))).pure[Future],
                reportAnotherDisposal =>
                  for {
                    updatedUserAnswers <- request.userAnswers
                      .setWhen(!reportAnotherDisposal)(OtherAssetsDisposalCompleted(srn), SectionCompleted)
                      .mapK[Future]
                    _ <- saveService.save(updatedUserAnswers)
                    submissionResult <- if (!reportAnotherDisposal) {
                      psrSubmissionService.submitPsrDetailsWithUA(
                        srn,
                        updatedUserAnswers,
                        optFallbackCall = Some(
                          controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                            .onPageLoad(srn, page)
                        )
                      )
                    } else {
                      Future.successful(Some(()))
                    }
                  } yield submissionResult.getOrRecoverJourney(
                    _ =>
                      Redirect(
                        navigator.nextPage(
                          ReportedOtherAssetsDisposalListPage(srn, reportAnotherDisposal),
                          mode,
                          request.userAnswers
                        )
                      )
                  )
              )
          }
        }
        .leftMap(_.pure[Future])
        .merge
  }

  private def getCompletedDisposals(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(OtherAssetsDisposalProgress.all(srn))
      .map {
        case (key, secondaryMap) =>
          key -> secondaryMap.filter { case (_, status) => status.completed }
      }
      .toList
      .traverse {
        case (key, sectionCompleted) =>
          for {
            otherAssetsIndex <- refineStringIndex[Max5000.Refined](key).getOrRecoverJourney
            disposalIndexes <- sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)
          } yield (otherAssetsIndex, disposalIndexes)
      }
      .map(_.toMap)
}

object ReportedOtherAssetsDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "assetDisposal.reportedOtherAssetsDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers
  ): List[ListRow] =
    disposals
      .flatMap {
        case (otherAssetsIndex, disposalIndexes) =>
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
      .toList
      .sortBy(_.changeUrl)

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
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers
  ): FormPageViewModel[ListViewModel] = {

    val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
    val numberOfOtherAssetsItems = userAnswers.map(OtherAssetsCompleted.all(srn)).size
    val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems

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
        rows(srn, disposals, userAnswers),
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
