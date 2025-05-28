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

package controllers.nonsipp.common

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import models.IdentityType._
import config.RefinedTypes.Max5000
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models._
import play.api.data.Form
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.OtherAssetsCYAPointOfEntry
import models.PointOfEntry.{NoPointOfEntry, WhoWasAssetAcquiredFromPointOfEntry}
import views.html.RadioListView
import models.SchemeId.Srn
import utils.IntUtils.IntOpts
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import controllers.nonsipp.common.IdentityTypeController._
import pages.nonsipp.common.IdentityTypePage
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IdentityTypeController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(
    srn: Srn,
    index: Int,
    mode: Mode,
    subject: IdentitySubject
  ): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    subject match {
      case IdentitySubject.Unknown => Redirect(controllers.routes.UnauthorisedController.onPageLoad())
      case _ =>
        // If this page is reached in CheckMode, in Other Assets Journey, and there is no PointOfEntry set
        if (mode == CheckMode && subject == IdentitySubject.OtherAssetSeller && request.userAnswers
            .get(OtherAssetsCYAPointOfEntry(srn, index.refined))
            .contains(NoPointOfEntry)) {
          // Set this page as the PointOfEntry
          saveService.save(
            request.userAnswers
              .set(OtherAssetsCYAPointOfEntry(srn, index.refined), WhoWasAssetAcquiredFromPointOfEntry)
              .getOrElse(request.userAnswers)
          )
        }

        val form = IdentityTypeController.form(formProvider, subject)
        Ok(
          view(
            form.fromUserAnswers(IdentityTypePage(srn, index.refined, subject)),
            viewModel(srn, index.refined, mode, subject, request.userAnswers)
          )
        )
    }
  }

  def onSubmit(
    srn: Srn,
    index: Int,
    mode: Mode,
    subject: IdentitySubject
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = IdentityTypeController.form(formProvider, subject)
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future
            .successful(
              BadRequest(view(formWithErrors, viewModel(srn, index.refined, mode, subject, request.userAnswers)))
            ),
        answer => {
          for {
            updatedAnswers <- request.userAnswers.set(IdentityTypePage(srn, index.refined, subject), answer).mapK
            nextPage = navigator.nextPage(IdentityTypePage(srn, index.refined, subject), mode, updatedAnswers)
            updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage, subject)
            _ <- saveService.save(updatedProgressAnswers)
          } yield Redirect(nextPage)
        }
      )
  }
}

object IdentityTypeController {

  def form(formProvider: RadioListFormProvider, subject: IdentitySubject): Form[IdentityType] = formProvider(
    s"${subject.key}.identityType.error.required"
  )

  private def radioListItems(subject: IdentitySubject): List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent"), Individual.name),
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent1"), UKCompany.name),
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent2"), UKPartnership.name),
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent3"), Other.name)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject,
    userAnswers: UserAnswers
  ): FormPageViewModel[RadioListViewModel] = {
    val text = subject match {
      case IdentitySubject.LoanRecipient => ""
      case IdentitySubject.LandOrPropertySeller =>
        userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)) match {
          case Some(value) => value.addressLine1
          case None => ""
        }
      case IdentitySubject.SharesSeller =>
        userAnswers.get(CompanyNameRelatedSharesPage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case _ => ""
    }
    FormPageViewModel(
      Message(s"${subject.key}.identityType.title"),
      Message(s"${subject.key}.identityType.heading", text),
      RadioListViewModel(
        None,
        radioListItems(subject)
      ),
      controllers.nonsipp.common.routes.IdentityTypeController
        .onSubmit(srn, index.value, mode, subject)
    )
  }
}
