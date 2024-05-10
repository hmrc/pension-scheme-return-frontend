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

package controllers.nonsipp.memberpensionpayments

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MembersDetailsPages
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.Max300
import controllers.PSRController
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import play.api.i18n.MessagesApi
import play.api.data.Form
import controllers.nonsipp.memberpensionpayments.PensionPaymentsReceivedController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments.{PensionPaymentsReceivedPage, TotalAmountPensionPaymentsPage}
import controllers.actions._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PensionPaymentsReceivedController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = PensionPaymentsReceivedController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(PensionPaymentsReceivedPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode)))),
        value =>
          if (value) {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PensionPaymentsReceivedPage(srn), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(PensionPaymentsReceivedPage(srn), mode, updatedAnswers))
          } else {
            val memberMap = request.userAnswers.map(MembersDetailsPages(srn))
            val maxIndex: Either[Result, Int] = memberMap.keys
              .map(_.toInt)
              .maxOption
              .map(Right(_))
              .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

            val optionList: List[Option[NameDOB]] = maxIndex match {
              case Right(index) =>
                (0 to index).toList.map { index =>
                  val memberOption = memberMap.get(index.toString)
                  memberOption match {
                    case Some(member) => Some(member)
                    case None => None
                  }
                }
              case Left(_) => List.empty
            }

            val zippedOptionList: List[(Max300, Option[NameDOB])] = optionList
              .zipWithRefinedIndexToList[Max300.Refined]

            val pensionPaymentsPages: List[UserAnswers.Compose] = zippedOptionList
              .flatMap {
                case (index, Some(_)) => List(_.set(TotalAmountPensionPaymentsPage(srn, index), Money(0)))
                case _ => Nil
              }

            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(PensionPaymentsReceivedPage(srn), value).compose(pensionPaymentsPages)
              )
              _ <- saveService.save(updatedAnswers)
              submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                srn,
                updatedAnswers,
                fallbackCall = controllers.nonsipp.memberpensionpayments.routes.PensionPaymentsReceivedController
                  .onPageLoad(srn, mode)
              )
            } yield submissionResult.getOrRecoverJourney(
              _ => Redirect(navigator.nextPage(PensionPaymentsReceivedPage(srn), mode, updatedAnswers))
            )
          }
      )
  }
}

object PensionPaymentsReceivedController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "pensionPaymentsReceived.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "pensionPaymentsReceived.title",
    Message("pensionPaymentsReceived.heading", schemeName),
    routes.PensionPaymentsReceivedController.onSubmit(srn, mode)
  )
}
