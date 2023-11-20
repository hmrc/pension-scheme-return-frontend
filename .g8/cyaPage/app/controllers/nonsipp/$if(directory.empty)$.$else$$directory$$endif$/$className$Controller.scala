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

$! Generic imports !$
$if(directory.empty)$

package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

$if(directory.empty)$
import pages.nonsipp.$className$Page
import controllers.nonsipp.$className;format="cap"$Controller._
$else$
import pages.nonsipp.$directory$.$className$CompletedPage
import controllers.nonsipp.$directory$.$className;format="cap"$Controller._
$endif$

$if(!index.empty)$
import config.Refined._
$endif$

import controllers.actions._
import models._
import models.SchemeId.Srn
import navigation.Navigator
import viewmodels.models._
import viewmodels.implicits._
import services.SaveService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import play.api.i18n.MessagesApi

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
$! Generic imports end!$

import views.html.CheckYourAnswersView

class $className;format="cap"$Controller @Inject()(
   override val messagesApi: MessagesApi,
   @Named("non-sipp") navigator: Navigator,
   identifyAndRequireData: IdentifyAndRequireData,
   val controllerComponents: MessagesControllerComponents,
   saveService: SaveService,
   view: CheckYourAnswersView
)(implicit ec: ExecutionContext) extends PSRController {

  def onPageLoad(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
      Ok(view(viewModel(srn, $if(!index.empty)index, $endif$$if(!secondaryIndex.empty)secondaryIndex, $endif$mode)))
  }

  def onSubmit(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set($className$CompletedPage(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), SectionCompleted))
      _              <- saveService.save(updatedAnswers)
    } yield Redirect(navigator.nextPage($className$CompletedPage(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), mode, updatedAnswers))
  }
}

object $className;format="cap"$Controller {
  def viewModel(srn: Srn, requiredPage: ???, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): FormPageViewModel[CheckYourAnswersViewModel] = FormPageViewModel[CheckYourAnswersViewModel](
    title = "checkYourAnswers.title",
    heading = "checkYourAnswers.heading",
    description = Some(ParagraphMessage("$className;format="decap"$.paragraph")),
    page = CheckYourAnswersViewModel(rows(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$)),
    refresh = None,
    buttonText = "site.continue",
    onSubmit = routes.$className$Controller.onSubmit(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$, mode)
  )

  private def rows(srn: Srn$if(!index.empty)$, index: $index$$endif$$if(!secondaryIndex.empty)$, secondaryIndex: $secondaryIndex$$endif$): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      None,
      List(
        CheckYourAnswersRowViewModel("row 1 key", "row 1 value")
          .withAction(
            SummaryAction("site.change", controllers.routes.UnauthorisedController.onPageLoad().url)
              .withVisuallyHiddenContent("row 1 key")
          ),
        CheckYourAnswersRowViewModel("row 2 key", "row 2 value")
          .withAction(
            SummaryAction("site.change", controllers.routes.UnauthorisedController.onPageLoad().url)
              .withVisuallyHiddenContent("row 2 key")
          )
      )
    )
  )
}
