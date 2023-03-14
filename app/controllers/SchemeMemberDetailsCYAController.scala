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

package controllers

import cats.implicits.{toShow, toTraverseOps}
import com.google.inject.Inject
import controllers.SchemeMemberDetailsCYAController._
import controllers.actions._
import models.SchemeId.Srn
import models.{Mode, NameDOB, NormalMode}
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.models._
import views.html.CheckYourAnswersView
import cats.syntax.either._
import config.Refined.Max99
import uk.gov.hmrc.domain.Nino
import utils.MessageUtils.booleanToMessage
import viewmodels.DisplayMessage.SimpleMessage

class SchemeMemberDetailsCYAController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  navigator: Navigator,
                                                  identify: IdentifierAction,
                                                  allowAccess: AllowAccessActionProvider,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: CheckYourAnswersView
                                                ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] =
    (identify andThen allowAccess(srn) andThen getData andThen requireData) {
      implicit request =>
        val result = for {
          memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index))
          hasNINO       <- request.userAnswers.get(NationalInsuranceNumberPage(srn, index))
          maybeNino     <- Option.when(hasNINO)(request.userAnswers.get(MemberDetailsNinoPage(srn, index))).sequence
        } yield Ok(view(viewModel(index, srn, mode, memberDetails, hasNINO, maybeNino)))

        result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] =
    (identify andThen allowAccess(srn) andThen getData andThen requireData) {
      implicit request =>
        Redirect(navigator.nextPage(SchemeMemberDetailsCYAPage(srn), NormalMode, request.userAnswers))
    }
}

object SchemeMemberDetailsCYAController {

  private def rows(
                    index: Max99,
                    srn: Srn,
                    mode: Mode,
                    memberDetails: NameDOB,
                    hasNINO: Boolean,
                    maybeNino: Option[Nino]): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("memberDetails.firstName", memberDetails.firstName)
      .withAction(
        SummaryAction("site.change", routes.MemberDetailsController.onPageLoad(srn, index, mode).url)
          .withVisuallyHiddenContent("memberDetails.firstName")
      ),
    CheckYourAnswersRowViewModel("memberDetails.lastName", memberDetails.lastName)
      .withAction(
        SummaryAction("site.change", routes.MemberDetailsController.onPageLoad(srn, index, mode).url)
          .withVisuallyHiddenContent("memberDetails.lastName")
      ),
    CheckYourAnswersRowViewModel("memberDetails.dateOfBirth", memberDetails.dob.show)
      .withAction(
        SummaryAction("site.change", routes.MemberDetailsController.onPageLoad(srn, index, mode).url)
          .withVisuallyHiddenContent("memberDetails.dateOfBirth")
      ),
    CheckYourAnswersRowViewModel(SimpleMessage("nationalInsuranceNumber.heading", memberDetails.fullName), booleanToMessage(hasNINO))
      .withAction(
        SummaryAction("site.change", routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, mode).url)
          .withVisuallyHiddenContent("site.endDate")
      )
  ) ++ ninoRow(maybeNino, memberDetails.fullName, srn, index, mode)

  private def ninoRow(maybeNino: Option[Nino], memberName: String, srn: Srn, index: Max99, mode: Mode): List[CheckYourAnswersRowViewModel] =
    maybeNino.fold(List.empty[CheckYourAnswersRowViewModel])(nino => List(
      CheckYourAnswersRowViewModel(SimpleMessage("memberDetailsNino.heading", memberName), nino.value)
        .withAction(
          SummaryAction("site.change", routes.MemberDetailsNinoController.onPageLoad(srn, index, mode).url)
            .withVisuallyHiddenContent("site.endDate")
        )
    ))

  def viewModel(
     index: Max99,
     srn: Srn, mode: Mode,
     memberDetails: NameDOB,
     hasNINO: Boolean,
     maybeNino: Option[Nino]
   ): CheckYourAnswersViewModel = CheckYourAnswersViewModel(
    rows(index, srn, mode, memberDetails, hasNINO, maybeNino),
    controllers.routes.SchemeMemberDetailsCYAController.onSubmit(srn, index)
  )
}
