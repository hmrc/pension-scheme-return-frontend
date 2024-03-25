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

package controllers.nonsipp.bonds

import cats.implicits.toShow
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.bonds.UnregulatedOrConnectedBondsHeldCYAController._
import models.SchemeHoldBond.{Acquisition, Contribution, Transfer}
import models.SchemeId.Srn
import models.{CheckMode, Mode, Money, NormalMode, SchemeHoldBond}
import navigation.Navigator
import pages.nonsipp.bonds._
import play.api.i18n._
import play.api.mvc._
import services.PsrSubmissionService
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class UnregulatedOrConnectedBondsHeldCYAController @Inject()(
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
    index: Max5000,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          nameOfBonds <- request.userAnswers.get(NameOfBondsPage(srn, index)).getOrRecoverJourney
          whyDoesSchemeHoldBonds <- request.userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, index)).getOrRecoverJourney

          whenDidSchemeAcquireBonds = Option.when(whyDoesSchemeHoldBonds != Transfer)(
            request.userAnswers.get(WhenDidSchemeAcquireBondsPage(srn, index)).get
          )

          costOfBonds <- request.userAnswers.get(CostOfBondsPage(srn, index)).getOrRecoverJourney

          bondsFromConnectedParty = Option.when(whyDoesSchemeHoldBonds == Acquisition)(
            request.userAnswers.get(BondsFromConnectedPartyPage(srn, index)).get
          )

          areBondsUnregulated <- request.userAnswers.get(AreBondsUnregulatedPage(srn, index)).getOrRecoverJourney

          incomeFromBonds <- request.userAnswers.get(IncomeFromBondsPage(srn, index)).getOrRecoverJourney

          schemeName = request.schemeDetails.schemeName
        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                nameOfBonds,
                whyDoesSchemeHoldBonds,
                whenDidSchemeAcquireBonds,
                costOfBonds,
                bondsFromConnectedParty,
                areBondsUnregulated,
                incomeFromBonds,
                mode
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService.submitPsrDetails(srn).map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(UnregulatedOrConnectedBondsHeldCYAPage(srn), NormalMode, request.userAnswers))
      }
    }
}

case class ViewModelParameters(
  srn: Srn,
  index: Max5000,
  schemeName: String,
  nameOfBonds: String,
  whyDoesSchemeHoldBonds: SchemeHoldBond,
  whenDidSchemeAcquireBonds: Option[LocalDate],
  costOfBonds: Money,
  bondsFromConnectedParty: Option[Boolean],
  areBondsUnregulated: Boolean,
  incomeFromBonds: Money,
  mode: Mode
)
object UnregulatedOrConnectedBondsHeldCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.mode
        .fold(normal = "bonds.checkYourAnswers.title", check = "bonds.checkYourAnswers.change.title"),
      heading = parameters.mode.fold(
        normal = "bonds.checkYourAnswers.heading",
        check = Message(
          "bonds.checkYourAnswers.change.heading"
        )
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.index,
          parameters.schemeName,
          parameters.nameOfBonds,
          parameters.whyDoesSchemeHoldBonds,
          parameters.whenDidSchemeAcquireBonds,
          parameters.costOfBonds,
          parameters.bondsFromConnectedParty,
          parameters.areBondsUnregulated,
          parameters.incomeFromBonds,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.mode.fold(normal = "site.saveAndContinue", check = "site.continue"),
      onSubmit = routes.UnregulatedOrConnectedBondsHeldCYAController
        .onSubmit(parameters.srn, parameters.mode)
    )

  private def sections(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    nameOfBonds: String,
    whyDoesSchemeHoldBonds: SchemeHoldBond,
    whenDidSchemeAcquireBonds: Option[LocalDate],
    costOfBonds: Money,
    bondsFromConnectedParty: Option[Boolean],
    areBondsUnregulated: Boolean,
    incomeFromBonds: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      whyDoesSchemeHoldBonds match {
        case Acquisition =>
          CheckYourAnswersSection(
            None,
            unconditionalRowsPart1(srn, index, schemeName, nameOfBonds, whyDoesSchemeHoldBonds, mode) ++
              List(whenDidSchemeAcquireBondsRow(srn, index, schemeName, whenDidSchemeAcquireBonds, mode)) ++
              unconditionalRowsPart2(srn, index, costOfBonds, mode) ++
              List(bondsFromConnectedPartyRow(srn, index, bondsFromConnectedParty, mode)) ++
              unconditionalRowsPart3(srn, index, areBondsUnregulated, incomeFromBonds, mode)
          )
        case Contribution =>
          CheckYourAnswersSection(
            None,
            unconditionalRowsPart1(srn, index, schemeName, nameOfBonds, whyDoesSchemeHoldBonds, mode) ++
              List(whenDidSchemeAcquireBondsRow(srn, index, schemeName, whenDidSchemeAcquireBonds, mode)) ++
              unconditionalRowsPart2(srn, index, costOfBonds, mode) ++
              unconditionalRowsPart3(srn, index, areBondsUnregulated, incomeFromBonds, mode)
          )
        case Transfer =>
          CheckYourAnswersSection(
            None,
            unconditionalRowsPart1(srn, index, schemeName, nameOfBonds, whyDoesSchemeHoldBonds, mode) ++
              unconditionalRowsPart2(srn, index, costOfBonds, mode) ++
              unconditionalRowsPart3(srn, index, areBondsUnregulated, incomeFromBonds, mode)
          )
      }
    )

  private def whenDidSchemeAcquireBondsRow(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    whenDidSchemeAcquireBonds: Option[LocalDate],
    mode: Mode
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.whenDidSchemeAcquireBonds", schemeName),
      whenDidSchemeAcquireBonds.get.show
    ).withAction(
      SummaryAction("site.change", routes.WhenDidSchemeAcquireBondsController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.whenDidSchemeAcquireBonds.hidden")
    )

  private def bondsFromConnectedPartyRow(
    srn: Srn,
    index: Max5000,
    bondsFromConnectedParty: Option[Boolean],
    mode: Mode
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.bondsFromConnectedParty", bondsFromConnectedParty.show),
      if (bondsFromConnectedParty.get) "site.yes" else "site.no"
    ).withAction(
      SummaryAction("site.change", routes.BondsFromConnectedPartyController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.bondsFromConnectedPartyPage.hidden")
    )

  private def unconditionalRowsPart1(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    nameOfBonds: String,
    whyDoesSchemeHoldBonds: SchemeHoldBond,
    mode: Mode
  ): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("bonds.checkYourAnswers.section.nameOfBonds", nameOfBonds.show).withAction(
      SummaryAction("site.change", routes.NameOfBondsController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.nameOfBonds.hidden")
    ),
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.whyDoesSchemeHoldBonds", schemeName),
      whyDoesSchemeHoldBonds match {
        case Acquisition => "bonds.checkYourAnswers.acquisition"
        case Contribution => "bonds.checkYourAnswers.contribution"
        case Transfer => "bonds.checkYourAnswers.transfer"
      }
    ).withAction(
      SummaryAction("site.change", routes.WhyDoesSchemeHoldBondsController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.whyDoesSchemeHoldBonds.hidden")
    )
  )

  private def unconditionalRowsPart2(
    srn: Srn,
    index: Max5000,
    costOfBonds: Money,
    mode: Mode
  ): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("bonds.checkYourAnswers.section.costOfBonds", s"£${costOfBonds.displayAs}")
      .withAction(
        SummaryAction("site.change", routes.CostOfBondsController.onSubmit(srn, index, mode).url)
          .withVisuallyHiddenContent("bonds.checkYourAnswers.section.costOfBonds.hidden")
      )
  )

  private def unconditionalRowsPart3(
    srn: Srn,
    index: Max5000,
    areBondsUnregulated: Boolean,
    incomeFromBonds: Money,
    mode: Mode
  ): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.areBondsUnregulated", areBondsUnregulated.show),
      if (areBondsUnregulated) "site.yes" else "site.no"
    ).withAction(
      SummaryAction("site.change", routes.AreBondsUnregulatedController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.areBondsUnregulated.hidden")
    ),
    CheckYourAnswersRowViewModel(
      "bonds.checkYourAnswers.section.incomeFromBonds",
      s"£${incomeFromBonds.displayAs}"
    ).withAction(
      SummaryAction("site.change", routes.IncomeFromBondsController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.incomeFromBonds.hidden")
    )
  )
}