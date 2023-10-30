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

import cats.implicits.toShow
import config.Refined.{Max300, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.memberdetails.SchemeMemberDetailsAnswersController._
import models.SchemeId.Srn
import models.{CheckMode, CheckOrChange, ConditionalYesNo, Mode, Money, NameDOB, NormalMode, Percentage}
import navigation.Navigator
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsPage, SchemeMemberDetailsAnswersPage}
import play.api.i18n._
import play.api.mvc._
import services.PsrSubmissionService
import uk.gov.hmrc.domain.Nino
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class SchemeMemberDetailsAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Max300,
    checkOrChange: CheckOrChange
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {

          memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
          hasNINO <- requiredPage(DoesMemberHaveNinoPage(srn, index))

          schemeName = request.schemeDetails.schemeName
        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                memberDetails,
                hasNINO,
                checkOrChange
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService.submitPsrDetails(srn).map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(SchemeMemberDetailsAnswersPage(srn), NormalMode, request.userAnswers))
      }
    }
}

case class ViewModelParameters(
  srn: Srn,
  index: Max300,
  schemeName: String,
  memberDetails: NameDOB,
  hasNINO: ConditionalYesNo[String, Nino],
  checkOrChange: CheckOrChange
)
object SchemeMemberDetailsAnswersController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.checkOrChange
        .fold(check = "checkYourAnswers.title", change = "changeMemberDetails.title"),
      heading = parameters.checkOrChange.fold(
        check = "checkYourAnswers.heading",
        change = Message(
          "changeMemberDetails.heading",
          parameters.memberDetails.fullName
        )
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.index,
          parameters.schemeName,
          parameters.memberDetails,
          parameters.hasNINO,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.checkOrChange.fold(check = "site.saveAndContinue", change = "site.continue"),
      onSubmit = routes.SchemeMemberDetailsAnswersController.onSubmit(parameters.srn, parameters.checkOrChange)
    )

  private def sections(
    srn: Srn,
    index: Max300,
    schemeName: String,
    memberDetails: NameDOB,
    hasNINO: ConditionalYesNo[String, Nino],
    mode: Mode
  ): List[CheckYourAnswersSection] =
    checkYourAnswerSection(
      srn,
      index,
      schemeName,
      memberDetails,
      hasNINO,
      mode
    )

  private def checkYourAnswerSection(
    srn: Srn,
    index: Max300,
    schemeName: String,
    memberDetails: NameDOB,
    hasNINO: ConditionalYesNo[String, Nino],
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
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
            hasNINO.value match {
              case Right(_) => "site.yes"
              case Left(_) => "site.no"
            }
          ).withAction(
            SummaryAction(
              "site.change",
              routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, CheckMode).url
            ).withVisuallyHiddenContent("nationalInsuranceNumber.heading")
          ),
          hasNINO.value match {
            case Right(titleNumber) =>
              CheckYourAnswersRowViewModel(
                Message("memberDetailsNino.heading", memberDetails.fullName),
                titleNumber.nino
              ).withAction(
                SummaryAction(
                  "site.change",
                  routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, mode).url
                ).withVisuallyHiddenContent("memberDetailsNino.heading")
              )
            case Left(reason) =>
              CheckYourAnswersRowViewModel(
                Message("noNINO.heading", memberDetails.fullName),
                reason.show
              ).withAction(
                SummaryAction(
                  "site.change",
                  routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, mode).url
                ).withVisuallyHiddenContent("noNINO.heading")
              )
          }
        )
      )
    )

}
