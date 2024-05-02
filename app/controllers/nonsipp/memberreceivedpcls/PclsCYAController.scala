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

package controllers.nonsipp.memberreceivedpcls

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined._
import controllers.PSRController
import controllers.nonsipp.memberreceivedpcls.PclsCYAController._
import controllers.actions._
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls.{PclsCYAPage, PensionCommencementLumpSumAmountPage}
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.ExecutionContext

import javax.inject.{Inject, Named}

class PclsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
          amounts <- request.userAnswers
            .get(PensionCommencementLumpSumAmountPage(srn, index))
            .getOrRecoverJourney
        } yield {
          Ok(view(viewModel(srn, memberDetails.fullName, index, amounts, mode)))
        }
      ).merge
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService
        .submitPsrDetails(
          srn,
          optFallbackCall = Some(
            controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController.onPageLoad(srn, index, mode)
          )
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) =>
            Redirect(navigator.nextPage(PclsCYAPage(srn, index), mode, request.userAnswers))
        }
    }
}

object PclsCYAController {
  def viewModel(
    srn: Srn,
    memberName: String,
    index: Max300,
    amounts: PensionCommencementLumpSum,
    mode: Mode
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = mode.fold(
        normal = "checkYourAnswers.title",
        check = Message("pclsCYA.change.title.check", memberName)
      ),
      heading = mode.fold(
        normal = "checkYourAnswers.heading",
        check = Message("pclsCYA.heading.check", memberName)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections = rows(srn, memberName, index, amounts, mode)
      ),
      refresh = None,
      buttonText = "site.saveAndContinue",
      onSubmit = routes.PclsCYAController.onSubmit(srn, index, mode)
    )

  private def rows(
    srn: Srn,
    memberName: String,
    index: Max300,
    amounts: PensionCommencementLumpSum,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.membersName"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.amount.received", memberName),
            Message("£" + amounts.lumpSumAmount.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                .onPageLoad(srn, index, CheckMode)
                .url + "#received"
            ).withVisuallyHiddenContent(Message("pclsCYA.rows.amount.received.hidden", memberName))
          ),
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.amount.relevant", memberName),
            Message("£" + amounts.designatedPensionAmount.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                .onPageLoad(srn, index, mode)
                .url + "#relevant"
            ).withVisuallyHiddenContent(Message("pclsCYA.rows.amount.relevant.hidden", memberName))
          )
        )
      )
    )
}
