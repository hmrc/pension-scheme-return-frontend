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
import pages.nonsipp.$directory$.$className$Page
import controllers.nonsipp.$directory$.$className;format="cap"$Controller._
$endif$

$if(!index.empty)$
import config.Refined._
$endif$

import controllers.actions._
import models._
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.models._
import viewmodels.implicits._
import services.SaveService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
$! Generic imports end!$

import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import views.html.DatePageView
import java.time.LocalDate
import play.api.i18n.{Messages, MessagesApi}
import config.Constants
import cats.implicits.toShow
import services.SchemeDateService
import utils.DateTimeUtils.localDateShow
import java.time.format.{DateTimeFormatter, FormatStyle}

class $className$Controller @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: DatePageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  schemeDateService: SchemeDateService,
  view: DatePageView
)(implicit ec: ExecutionContext) extends PSRController {

  private def form(date: LocalDate)(implicit messages: Messages) = $className;format="cap"$Controller.form(formProvider, date)

  $! Not Geniric (form takes date) !$
  def onPageLoad(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        val preparedForm = request.userAnswers.get($className$Page(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$)).fold(form(date.to))(form(date.to).fill)
        $if(!requiredPage) $
          request.userAnswers.get($requiredPage$(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$)).getOrRecoverJourney { requiredPage =>
            $endif$
            Ok(view(preparedForm, viewModel(srn, $if(!requiredPage) $requiredPage, $endif$$if(!index.empty) $index, $endif$$if(!secondaryIndex.empty) $secondaryIndex, $endif$mode)))
            $if(!requiredPage) $
          }
        $endif$
      }
  }

  def onSubmit(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        form(date.to).bindFromRequest().fold(
          formWithErrors => {
            $if(!requiredPage) $
              request.userAnswers.get($requiredPage$(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$)).getOrRecoverJourney { requiredPage =>
                $endif$
                Future.successful(BadRequest(view(formWithErrors, viewModel(srn, $if(!requiredPage) $requiredPage, $endif$$if(!index.empty) $index, $endif$$if(!secondaryIndex.empty) $secondaryIndex, $endif$mode))))
                $if(!requiredPage) $
              }
            $endif$
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage($className$Page(srn$if(!index.empty) $, index$endif$$if(!secondaryIndex.empty) $, secondaryIndex$endif$), mode, updatedAnswers))
        )
      }
  }
  $! Generic functions end !$
}

object $className;format="cap"$Controller {
  def form(formProvider: DatePageFormProvider, date: LocalDate)(implicit messages: Messages): Form[LocalDate] = formProvider(
    DateFormErrors(
      required = "$className;format="decap"$.error.required.all",
      requiredDay = "$className;format="decap"$.error.required.day",
      requiredMonth = "$className;format="decap"$.error.required.month",
      requiredYear = "$className;format="decap"$.error.required.year",
      requiredTwo = "$className;format="decap"$.error.required.two",
      invalidDate = "$className;format="decap"$.error.invalid.date",
      invalidCharacters = "$className;format="decap"$.error.invalid.chars",
      validators = List(
        DateFormErrors
          .failIfDateAfter(date, messages("$className;format="decap"$.error.date.after", date.show)),
        DateFormErrors
          .failIfDateBefore(
            Constants.earliestDate,
            messages(
              "$className;format="decap"$.error.date.before",
              Constants.earliestDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            )
          )
      )
    )
  )

  def viewModel(srn: Srn, $if(!requiredPage.empty)$requiredPage: ???, $endif$$if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): FormPageViewModel[DatePageViewModel] = {
    FormPageViewModel(
      "$className;format="decap"$.title",
      "$className;format="decap"$.heading",
      DatePageViewModel(),
      $! Generic onSubmit !$
      $if(directory.empty)$
      controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
      $else$
      controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
      $endif$
      $! Generic onSubmit end !$
    )
  }
}
