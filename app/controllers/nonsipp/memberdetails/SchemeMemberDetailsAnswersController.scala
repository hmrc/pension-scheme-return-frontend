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

package controllers.nonsipp.memberdetails

import cats.implicits.{toShow, toTraverseOps}
import com.google.inject.Inject
import config.Refined.Max99
import controllers.actions._
import controllers.nonsipp.memberdetails.SchemeMemberDetailsAnswersController._
import models.SchemeId.Srn
import models.{CheckMode, CheckOrChange, Mode, NameDOB, NormalMode}
import navigation.Navigator
import pages._
import pages.nonsipp.memberdetails._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import utils.MessageUtils.booleanToMessage
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import javax.inject.Named

class SchemeMemberDetailsAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max99, checkOrChange: CheckOrChange, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      val result = for {
        memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index))
        hasNINO <- request.userAnswers.get(DoesMemberHaveNinoPage(srn, index))
        maybeNino <- Option.when(hasNINO)(request.userAnswers.get(MemberDetailsNinoPage(srn, index))).sequence
        maybeNoNinoReason <- Option.when(!hasNINO)(request.userAnswers.get(NoNINOPage(srn, index))).sequence
      } yield Ok(view(viewModel(index, srn, mode, checkOrChange, memberDetails, hasNINO, maybeNino, maybeNoNinoReason)))

      result.getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(srn: Srn, index: Max99, checkOrChange: CheckOrChange, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(SchemeMemberDetailsAnswersPage(srn), NormalMode, request.userAnswers))
    }
}

object SchemeMemberDetailsAnswersController {

  private def rows(
    index: Max99,
    srn: Srn,
    mode: Mode,
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
    index: Max99
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
    index: Max99
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
    index: Max99,
    srn: Srn,
    mode: Mode,
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
      CheckYourAnswersViewModel(rows(index, srn, mode, memberDetails, hasNINO, maybeNino, maybeNoNinoReason)),
      routes.SchemeMemberDetailsAnswersController.onSubmit(srn, index, checkOrChange)
    ).withButtonText(Message("site.continue"))
}
