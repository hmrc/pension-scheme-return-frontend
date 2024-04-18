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

package controllers.nonsipp.memberdetails

import services.{PsrSubmissionService, SaveService}
import controllers.nonsipp.memberdetails.SchemeMemberDetailsAnswersController._
import pages.nonsipp.memberdetails._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import config.Refined.Max300
import controllers.PSRController
import cats.implicits.{toShow, toTraverseOps}
import controllers.actions._
import play.api.i18n.MessagesApi
import viewmodels.implicits._
import utils.MessageUtils.booleanToMessage
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class SchemeMemberDetailsAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max300, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val result = for {
        memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index))
        hasNINO <- request.userAnswers.get(DoesMemberHaveNinoPage(srn, index))
        maybeNino <- Option.when(hasNINO)(request.userAnswers.get(MemberDetailsNinoPage(srn, index))).sequence
        maybeNoNinoReason <- Option.when(!hasNINO)(request.userAnswers.get(NoNINOPage(srn, index))).sequence
      } yield Ok(view(viewModel(index, srn, checkOrChange, memberDetails, hasNINO, maybeNino, maybeNoNinoReason)))

      result.getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(srn: Srn, index: Max300, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedUserAnswers <- Future.fromTry(request.userAnswers.set(MemberStatus(srn, index), MemberState.Active))
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(srn, updatedUserAnswers)
      } yield submissionResult.fold(
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      )(_ => Redirect(navigator.nextPage(SchemeMemberDetailsAnswersPage(srn), NormalMode, request.userAnswers)))
    }
}

object SchemeMemberDetailsAnswersController {

  private def rows(
    index: Max300,
    srn: Srn,
    memberDetails: NameDOB,
    hasNINO: Boolean,
    maybeNino: Option[Nino],
    maybeNoNinoReason: Option[String]
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel("memberDetails.firstName", memberDetails.firstName)
        .withAction(
          SummaryAction("site.change", routes.MemberDetailsController.onPageLoad(srn, index, CheckMode).url)
            .withVisuallyHiddenContent("memberDetails.firstName")
        ),
      CheckYourAnswersRowViewModel("memberDetails.lastName", memberDetails.lastName)
        .withAction(
          SummaryAction("site.change", routes.MemberDetailsController.onPageLoad(srn, index, CheckMode).url)
            .withVisuallyHiddenContent("memberDetails.lastName")
        ),
      CheckYourAnswersRowViewModel("memberDetails.dateOfBirth", memberDetails.dob.show)
        .withAction(
          SummaryAction("site.change", routes.MemberDetailsController.onPageLoad(srn, index, CheckMode).url)
            .withVisuallyHiddenContent("memberDetails.dateOfBirth")
        ),
      CheckYourAnswersRowViewModel(
        Message("nationalInsuranceNumber.heading", memberDetails.fullName),
        booleanToMessage(hasNINO)
      ).withAction(
        SummaryAction("site.change", routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, CheckMode).url)
          .withVisuallyHiddenContent("nationalInsuranceNumber.heading")
      )
    ) ++
      ninoRow(maybeNino, memberDetails.fullName, srn, index) ++
      noNinoReasonRow(maybeNoNinoReason, memberDetails.fullName, srn, index)

  private def ninoRow(
    maybeNino: Option[Nino],
    memberName: String,
    srn: Srn,
    index: Max300
  ): List[CheckYourAnswersRowViewModel] =
    maybeNino.fold(List.empty[CheckYourAnswersRowViewModel])(
      nino =>
        List(
          CheckYourAnswersRowViewModel(Message("memberDetailsNino.heading", memberName), nino.value)
            .withAction(
              SummaryAction("site.change", routes.MemberDetailsNinoController.onPageLoad(srn, index, CheckMode).url)
                .withVisuallyHiddenContent("memberDetailsNino.heading")
            )
        )
    )

  private def noNinoReasonRow(
    maybeNoNinoReason: Option[String],
    memberName: String,
    srn: Srn,
    index: Max300
  ): List[CheckYourAnswersRowViewModel] =
    maybeNoNinoReason.fold(List.empty[CheckYourAnswersRowViewModel])(
      noNinoReason =>
        List(
          CheckYourAnswersRowViewModel(Message("noNINO.heading", memberName), noNinoReason)
            .withAction(
              SummaryAction("site.change", routes.NoNINOController.onPageLoad(srn, index, CheckMode).url)
                .withVisuallyHiddenContent("noNINO.heading")
            )
        )
    )

  def viewModel(
    index: Max300,
    srn: Srn,
    checkOrChange: CheckOrChange,
    memberDetails: NameDOB,
    hasNINO: Boolean,
    maybeNino: Option[Nino],
    maybeNoNinoReason: Option[String]
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel(
      title = checkOrChange.fold(check = "checkYourAnswers.title", change = "changeMemberDetails.title"),
      heading = checkOrChange.fold(
        check = "checkYourAnswers.heading",
        change = Message("changeMemberDetails.heading", memberDetails.fullName)
      ),
      CheckYourAnswersViewModel.singleSection(
        rows(index, srn, memberDetails, hasNINO, maybeNino, maybeNoNinoReason)
      ),
      routes.SchemeMemberDetailsAnswersController.onSubmit(srn, index, checkOrChange)
    ).withButtonText(Message("site.continue"))
}
