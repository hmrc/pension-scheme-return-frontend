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

package controllers.nonsipp.membersurrenderedbenefits

import services.{SaveService, SchemeDateService}
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import utils.IntUtils.{toInt, toRefined300}
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsAmountPage, WhenDidMemberSurrenderBenefitsPage}
import cats.{Id, Monad}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import forms.mappings.errors.DateFormErrors
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import controllers.nonsipp.membersurrenderedbenefits.WhenDidMemberSurrenderBenefitsController._
import forms.DatePageFormProvider
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode}
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class WhenDidMemberSurrenderBenefitsController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: DatePageFormProvider,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: DatePageView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form(date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    WhenDidMemberSurrenderBenefitsController.form(formProvider, date)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      usingSchemeDate[Id](srn) { date =>
        request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { member =>
          request.userAnswers.get(SurrenderedBenefitsAmountPage(srn, index)).getOrRecoverJourney {
            surrenderedBenefitsAmount =>
              val preparedForm = request.userAnswers
                .get(WhenDidMemberSurrenderBenefitsPage(srn, index))
                .fold(form(date))(form(date).fill)
              Ok(
                view(
                  preparedForm,
                  viewModel(srn, index, member.fullName, surrenderedBenefitsAmount.displayAs, mode)
                )
              )
          }
        }
      }
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      usingSchemeDate(srn) { date =>
        request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { member =>
          request.userAnswers.get(SurrenderedBenefitsAmountPage(srn, index)).getOrRecoverJourney {
            surrenderedBenefitsAmount =>
              form(date)
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(
                        view(
                          formWithErrors,
                          viewModel(srn, index, member.fullName, surrenderedBenefitsAmount.displayAs, mode)
                        )
                      )
                    ),
                  value =>
                    for {
                      updatedAnswers <- request.userAnswers
                        .set(WhenDidMemberSurrenderBenefitsPage(srn, index), value)
                        .mapK[Future]
                      nextPage = navigator
                        .nextPage(WhenDidMemberSurrenderBenefitsPage(srn, index), mode, updatedAnswers)
                      updatedProgressAnswers <- saveProgress(
                        srn,
                        index,
                        updatedAnswers,
                        nextPage
                      )
                      _ <- saveService.save(updatedProgressAnswers)
                    } yield Redirect(nextPage)
                )
          }
        }
      }
    }

  private def usingSchemeDate[F[_]: Monad](
    srn: Srn
  )(body: DateRange => F[Result])(implicit request: DataRequest[?]): F[Result] =
    schemeDateService.schemeDate(srn) match {
      case Some(period) => body(period)
      case None => Monad[F].pure(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

}

object WhenDidMemberSurrenderBenefitsController {
  def form(formProvider: DatePageFormProvider, date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    formProvider(
      DateFormErrors(
        required = "surrenderedBenefits.when.error.required.all",
        requiredDay = "surrenderedBenefits.when.error.required.day",
        requiredMonth = "surrenderedBenefits.when.error.required.month",
        requiredYear = "surrenderedBenefits.when.error.required.year",
        requiredTwo = "surrenderedBenefits.when.error.required.two",
        invalidDate = "surrenderedBenefits.when.error.invalid.date",
        invalidCharacters = "surrenderedBenefits.when.error.invalid.characters",
        validators = List(
          DateFormErrors
            .failIfDateAfter(date.to, messages("surrenderedBenefits.when.error.date.after", date.to.show)),
          DateFormErrors
            .failIfDateBefore(date.from, messages("surrenderedBenefits.when.error.date.before", date.from.show))
        )
      )
    )

  def viewModel(
    srn: Srn,
    index: Max300,
    memberName: String,
    surrenderedBenefitsAmount: String,
    mode: Mode
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      title = Message("surrenderedBenefits.when.title"),
      heading = Message("surrenderedBenefits.when.heading", memberName, surrenderedBenefitsAmount),
      page = DatePageViewModel(
        None,
        Message("surrenderedBenefits.when.heading", memberName, surrenderedBenefitsAmount),
        Some("surrenderedBenefits.when.hint")
      ),
      onSubmit = routes.WhenDidMemberSurrenderBenefitsController.onSubmit(srn, index, mode)
    )
}
