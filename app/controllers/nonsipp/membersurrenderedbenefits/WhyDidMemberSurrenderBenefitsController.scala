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

import services.SaveService
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.TextFormProvider
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsAmountPage, WhyDidMemberSurrenderBenefitsPage}
import models.Mode
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.TextAreaView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextAreaViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhyDidMemberSurrenderBenefitsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextAreaView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val form = WhyDidMemberSurrenderBenefitsController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
        request.userAnswers.get(SurrenderedBenefitsAmountPage(srn, memberIndex)).getOrRecoverJourney { amount =>
          val preparedForm = {
            request.userAnswers.fillForm(WhyDidMemberSurrenderBenefitsPage(srn, memberIndex), form)
          }
          Ok(
            view(
              preparedForm,
              WhyDidMemberSurrenderBenefitsController
                .viewModel(
                  srn,
                  memberIndex,
                  mode,
                  request.schemeDetails.schemeName,
                  amount.displayAs,
                  memberName.fullName
                )
            )
          )
        }

      }
  }

  def onSubmit(srn: Srn, memberIndex: Max300, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
        request.userAnswers.get(SurrenderedBenefitsAmountPage(srn, memberIndex)).getOrRecoverJourney { amount =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      WhyDidMemberSurrenderBenefitsController.viewModel(
                        srn,
                        memberIndex,
                        mode,
                        request.schemeDetails.schemeName,
                        amount.displayAs,
                        memberName.fullName
                      )
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(WhyDidMemberSurrenderBenefitsPage(srn, memberIndex), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(
                  navigator.nextPage(WhyDidMemberSurrenderBenefitsPage(srn, memberIndex), mode, updatedAnswers)
                )
            )
        }
      }

  }
}

object WhyDidMemberSurrenderBenefitsController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textAreaShorter(
    "surrenderedBenefits.whyDidMemberSurrenderBenefits.error.required",
    "surrenderedBenefits.whyDidMemberSurrenderBenefits.error.length",
    "surrenderedBenefits.whyDidMemberSurrenderBenefits.error.invalid"
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    mode: Mode,
    schemeName: String,
    amount: String,
    lenderName: String
  ): FormPageViewModel[TextAreaViewModel] =
    FormPageViewModel(
      "surrenderedBenefits.whyDidMemberSurrenderBenefits.title",
      Message("surrenderedBenefits.whyDidMemberSurrenderBenefits.heading", schemeName, lenderName, amount),
      TextAreaViewModel(),
      controllers.nonsipp.membersurrenderedbenefits.routes.WhyDidMemberSurrenderBenefitsController
        .onSubmit(srn, index, mode)
    )
}
