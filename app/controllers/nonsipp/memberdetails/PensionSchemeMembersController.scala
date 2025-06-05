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

import services.SaveService
import pages.nonsipp.memberdetails.PensionSchemeMembersPage
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import models.ManualOrUpload.{Manual, Upload}
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import controllers.nonsipp.memberdetails.PensionSchemeMembersController._
import models.{ManualOrUpload, NormalMode}
import play.api.data.Form
import views.html.RadioListView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PensionSchemeMembersController @Inject() (
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

  private val form = PensionSchemeMembersController.form(formProvider)

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Ok(
      view(
        form.fromUserAnswers(PensionSchemeMembersPage(srn)),
        viewModel(srn, request.schemeDetails.schemeName)
      )
    )
  }

  def onSubmit(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName)))),
        answer =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PensionSchemeMembersPage(srn), answer))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(PensionSchemeMembersPage(srn), NormalMode, updatedAnswers))
      )
  }
}

object PensionSchemeMembersController {

  def form(formProvider: RadioListFormProvider): Form[ManualOrUpload] = formProvider(
    "pensionSchemeMembers.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("pensionSchemeMembers.manualEntry"), Manual.name),
      RadioListRowViewModel(
        Message("pensionSchemeMembers.upload"),
        Upload.name,
        Message("pensionSchemeMembers.upload.hint")
      )
    )

  def viewModel(srn: Srn, schemeName: String): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("pensionSchemeMembers.title", schemeName),
      Message("pensionSchemeMembers.heading", schemeName),
      RadioListViewModel(
        Some(Message("pensionSchemeMembers.legend")),
        radioListItems
      ),
      routes.PensionSchemeMembersController.onSubmit(srn)
    ).withDescription(
      ParagraphMessage("pensionSchemeMembers.description") ++
        ListMessage(
          ListType.Bullet,
          "pensionSchemeMembers.description.name",
          "pensionSchemeMembers.description.dob",
          "pensionSchemeMembers.description.nino"
        ) ++
        ParagraphMessage("pensionSchemeMembers.description1")
    )
}
