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

package controllers.nonsipp.bonds

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds._
import viewmodels.implicits._
import play.api.mvc._
import utils.IntUtils.toRefined5000
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import play.api.i18n._
import models.requests.DataRequest
import controllers.nonsipp.bonds.UnregulatedOrConnectedBondsHeldCYAController._
import controllers.PSRController
import models.SchemeHoldBond.{Acquisition, Contribution, Transfer}
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import utils.FunctionKUtils._
import viewmodels.DisplayMessage._
import viewmodels.models.SectionJourneyStatus.InProgress
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class UnregulatedOrConnectedBondsHeldCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Int, mode: Mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Int, mode: Mode)
    }

  def onPageLoadCommon(srn: Srn, index: Int, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers.get(BondsProgress(srn, index)) match {
      case Some(InProgress(_)) => Redirect(routes.BondsListController.onPageLoad(srn, 1, mode))
      case _ =>
        (
          for {
            nameOfBonds <- request.userAnswers.get(NameOfBondsPage(srn, index)).getOrRecoverJourney
            whyDoesSchemeHoldBonds <- request.userAnswers
              .get(WhyDoesSchemeHoldBondsPage(srn, index))
              .getOrRecoverJourney

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
                mode,
                viewOnlyUpdated = false,
                optYear = request.year,
                optCurrentVersion = request.currentVersion,
                optPreviousVersion = request.previousVersion
              )
            )
          )
        ).merge
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val prePopulated = request.userAnswers.get(BondPrePopulated(srn, index))
      for {
        updatedAnswers <- request.userAnswers
          .set(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .setWhen(prePopulated.isDefined)(BondPrePopulated(srn, index), true)
          .mapK
        _ <- saveService.save(updatedAnswers)
        result <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall =
              controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldCYAController.onPageLoad(srn, index, mode)
          )
      } yield result match {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(UnregulatedOrConnectedBondsHeldCYAPage(srn), NormalMode, request.userAnswers))
      }
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.bonds.routes.BondsListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object UnregulatedOrConnectedBondsHeldCYAController {
  def viewModel(
    srn: Srn,
    index: Int,
    schemeName: String,
    nameOfBonds: String,
    whyDoesSchemeHoldBonds: SchemeHoldBond,
    whenDidSchemeAcquireBonds: Option[LocalDate],
    costOfBonds: Money,
    bondsFromConnectedParty: Option[Boolean],
    areBondsUnregulated: Boolean,
    incomeFromBonds: Money,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "bonds.checkYourAnswers.title",
        check = "bonds.checkYourAnswers.change.title",
        viewOnly = "bonds.checkYourAnswers.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "bonds.checkYourAnswers.heading",
        check = "bonds.checkYourAnswers.change.heading",
        viewOnly = Message("bonds.checkYourAnswers.viewOnly.heading", nameOfBonds)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
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
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(
        normal = "site.saveAndContinue",
        check = "site.continue",
        viewOnly = "site.continue"
      ),
      onSubmit = routes.UnregulatedOrConnectedBondsHeldCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode.isViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "bonds.checkYourAnswers.viewOnly.title",
            heading = Message("bonds.checkYourAnswers.viewOnly.heading", nameOfBonds),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                routes.UnregulatedOrConnectedBondsHeldCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                routes.UnregulatedOrConnectedBondsHeldCYAController.onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def sections(
    srn: Srn,
    index: Int,
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
    index: Int,
    schemeName: String,
    whenDidSchemeAcquireBonds: Option[LocalDate],
    mode: Mode
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.whenDidSchemeAcquireBonds", schemeName),
      whenDidSchemeAcquireBonds.get.show
    ).withAction(
      SummaryAction("site.change", routes.WhenDidSchemeAcquireBondsController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent(("bonds.checkYourAnswers.section.whenDidSchemeAcquireBonds.hidden", schemeName))
    )

  private def bondsFromConnectedPartyRow(
    srn: Srn,
    index: Int,
    bondsFromConnectedParty: Option[Boolean],
    mode: Mode
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(
      Message("bonds.checkYourAnswers.section.bondsFromConnectedParty", bondsFromConnectedParty.show),
      if (bondsFromConnectedParty.get) "site.yes" else "site.no"
    ).withAction(
      SummaryAction("site.change", routes.BondsFromConnectedPartyController.onSubmit(srn, index, mode).url)
        .withVisuallyHiddenContent("bonds.checkYourAnswers.section.bondsFromConnectedParty.hidden")
    )

  private def unconditionalRowsPart1(
    srn: Srn,
    index: Int,
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
        .withVisuallyHiddenContent(("bonds.checkYourAnswers.section.whyDoesSchemeHoldBonds.hidden", schemeName))
    )
  )

  private def unconditionalRowsPart2(
    srn: Srn,
    index: Int,
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
    index: Int,
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
