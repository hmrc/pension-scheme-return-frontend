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

package controllers.nonsipp.otherassetsheld

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils._
import config.Refined.Max5000
import controllers.PSRController
import config.Constants
import cats.implicits.{catsSyntaxApplicativeId, toShow, toTraverseOps}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import controllers.nonsipp.otherassetsheld.OtherAssetsListController._
import models._
import pages.nonsipp.otherassetsheld._
import com.google.inject.Inject
import views.html.ListView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class OtherAssetsListController @Inject()(
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

  val form: Form[Boolean] = OtherAssetsListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val indexes: List[Max5000] =
        request.userAnswers.map(OtherAssetsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.nonEmpty) {
        otherAssetsData(srn, indexes).map { data =>
          val filledForm =
            request.userAnswers.get(OtherAssetsListPage(srn)).fold(form)(form.fill)
          Ok(view(filledForm, viewModel(srn, page, mode, data)))
        }.merge
      } else {
        Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val indexes: List[Max5000] =
        request.userAnswers.map(OtherAssetsCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

      if (indexes.size >= Constants.maxOtherAssetsTransactions) {
        Future.successful(
          Redirect(
            navigator.nextPage(OtherAssetsListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors => {
              otherAssetsData(srn, indexes)
                .map { data =>
                  BadRequest(view(errors, viewModel(srn, page, mode, data)))
                }
                .merge
                .pure[Future]
            },
            addAnother =>
              for {
                updatedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .set(
                        OtherAssetsJourneyStatus(srn),
                        if (!addAnother) SectionStatus.Completed else SectionStatus.InProgress
                      )
                      .set(OtherAssetsListPage(srn), addAnother)
                  )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (!addAnother) {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall =
                      controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, page, mode)
                  )
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(OtherAssetsListPage(srn), mode, updatedUserAnswers)
                  )
              )
          )
      }
  }

  private def otherAssetsData(srn: Srn, indexes: List[Max5000])(
    implicit req: DataRequest[_]
  ): Either[Result, List[OtherAssetsData]] =
    indexes
      .sortBy(x => x.value)
      .map { index =>
        for {
          nameOfOtherAssets <- requiredPage(WhatIsOtherAssetPage(srn, index))
        } yield OtherAssetsData(index, nameOfOtherAssets)
      }
      .sequence
}

object OtherAssetsListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "otherAssets.list.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[OtherAssetsData]
  ): List[ListRow] =
    memberList.map {
      case OtherAssetsData(index, nameOfOtherAssets) =>
        val otherAssetsMessage =
          Message("otherAssets.list.row", nameOfOtherAssets.show)

        ListRow(
          otherAssetsMessage,
          changeUrl = controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
            .onPageLoad(srn, index, CheckMode)
            .url,
          changeHiddenText = Message("bondsList.row.change.hiddenText", otherAssetsMessage),
          removeUrl = controllers.nonsipp.otherassetsheld.routes.RemoveOtherAssetController
            .onPageLoad(srn, index, NormalMode)
            .url,
          removeHiddenText = Message("bondsList.row.remove.hiddenText", otherAssetsMessage)
        )
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    data: List[OtherAssetsData]
  ): FormPageViewModel[ListViewModel] = {
    val title = if (data.size > 1) "otherAssets.list.title.plural" else "otherAssets.list.title"
    val heading = if (data.size > 1) "otherAssets.list.heading.plural" else "otherAssets.list.heading"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.pageSize,
      totalSize = data.size,
      call = controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, _, mode)
    )

    val conditionalInsetText: DisplayMessage = {
      if (data.size >= Constants.maxOtherAssetsTransactions) {
        ParagraphMessage("otherAssets.list.inset")
      } else {
        Message("")
      }
    }
    FormPageViewModel(
      title = Message(title, data.size),
      heading = Message(heading, data.size),
      description = Some(ParagraphMessage("otherAssets.list.description")),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows = rows(srn, mode, data),
        radioText = Message("otherAssets.list.radios"),
        showRadios = data.size < Constants.maxOtherAssetsTransactions,
        showInsetWithRadios = !(data.length < Constants.maxOtherAssetsTransactions),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "otherAssets.list.pagination.label",
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
      onSubmit = controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onSubmit(srn, page, mode)
    )
  }

  case class OtherAssetsData(
    index: Max5000,
    nameOfOtherAssets: String
  )

}
