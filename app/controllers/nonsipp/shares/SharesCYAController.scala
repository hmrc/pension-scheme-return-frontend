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

package controllers.nonsipp.shares

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import utils.ListUtils.ListOps
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import cats.implicits.toShow
import controllers.actions._
import controllers.nonsipp.shares.SharesCYAController._
import pages.nonsipp.common._
import play.api.i18n._
import models.requests.DataRequest
import pages.nonsipp.shares._
import play.api.mvc._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models.{SchemeHoldShare, _}
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Named}

class SharesCYAController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Max5000,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
    }

  def onPageLoadCommon(srn: SchemeId.Srn, index: Max5000, mode: Mode)(
    implicit request: DataRequest[AnyContent]
  ): Result =
    (
      for {
        typeOfShare <- requiredPage(TypeOfSharesHeldPage(srn, index))
        holdShares <- requiredPage(WhyDoesSchemeHoldSharesPage(srn, index))

        whenDidSchemeAcquire = Option.when(holdShares != Transfer)(
          request.userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)).get
        )
        companyNameRelatedShares <- requiredPage(CompanyNameRelatedSharesPage(srn, index))

        companySharesCrn <- requiredPage(SharesCompanyCrnPage(srn, index))

        classOfShares <- requiredPage(ClassOfSharesPage(srn, index))

        howManyShares <- requiredPage(HowManySharesPage(srn, index))

        identityType = Option.when(holdShares == Acquisition)(
          request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.SharesSeller)).get
        )

        sharesFromConnectedParty = Option.when(typeOfShare == Unquoted)(
          request.userAnswers.get(SharesFromConnectedPartyPage(srn, index)).get
        )

        recipientName = Option.when(holdShares == Acquisition)(
          List(
            request.userAnswers.get(IndividualNameOfSharesSellerPage(srn, index)),
            request.userAnswers.get(CompanyNameOfSharesSellerPage(srn, index)),
            request.userAnswers.get(PartnershipShareSellerNamePage(srn, index)),
            request.userAnswers
              .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller))
              .map(_.name)
          ).flatten.head
        )

        recipientDetails = Option.when(holdShares == Acquisition)(
          List(
            request.userAnswers
              .get(SharesIndividualSellerNINumberPage(srn, index))
              .flatMap(_.value.toOption.map(_.value)),
            request.userAnswers
              .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller))
              .flatMap(_.value.toOption.map(_.value)),
            request.userAnswers
              .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller))
              .flatMap(_.value.toOption.map(_.value)),
            request.userAnswers
              .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller))
              .map(_.description)
          ).flatten.headOption
        )

        recipientReasonNoDetails = Option.when(holdShares == Acquisition)(
          List(
            request.userAnswers
              .get(SharesIndividualSellerNINumberPage(srn, index))
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller))
              .flatMap(_.value.swap.toOption.map(_.value)),
            request.userAnswers
              .get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller))
              .flatMap(_.value.swap.toOption.map(_.value))
          ).flatten.headOption
        )

        costOfShares <- requiredPage(CostOfSharesPage(srn, index))

        shareIndependentValue <- requiredPage(SharesIndependentValuationPage(srn, index))

        totalAssetValue = Option.when(typeOfShare == SponsoringEmployer && holdShares == Acquisition)(
          request.userAnswers.get(TotalAssetValuePage(srn, index)).get
        )

        sharesTotalIncome <- requiredPage(SharesTotalIncomePage(srn, index))

        schemeName = request.schemeDetails.schemeName
      } yield Ok(
        view(
          viewModel(
            srn,
            index,
            schemeName,
            typeOfShare,
            holdShares,
            whenDidSchemeAcquire,
            companyNameRelatedShares,
            companySharesCrn,
            classOfShares,
            howManyShares,
            identityType,
            recipientName,
            recipientDetails.flatten,
            recipientReasonNoDetails.flatten,
            sharesFromConnectedParty,
            costOfShares,
            shareIndependentValue,
            totalAssetValue,
            sharesTotalIncome,
            mode,
            viewOnlyUpdated = false,
            optYear = request.year,
            optCurrentVersion = request.currentVersion,
            optPreviousVersion = request.previousVersion,
            compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
          )
        )
      )
    ).merge

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        updatedAnswers <- Future
          .fromTry(
            request.userAnswers
              .set(SharesCompleted(srn, index), SectionCompleted)
          )
        _ <- saveService.save(updatedAnswers)
        redirectTo <- psrSubmissionService
          .submitPsrDetails(
            srn,
            fallbackCall = controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, mode)
          )
          .map {
            case None => controllers.routes.JourneyRecoveryController.onPageLoad()
            case Some(_) => navigator.nextPage(SharesCYAPage(srn), NormalMode, request.userAnswers)
          }
      } yield Redirect(redirectTo)
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.shares.routes.SharesListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

case class ViewModelParameters(
  )
object SharesCYAController {
  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    typeOfShare: TypeOfShares,
    holdShares: SchemeHoldShare,
    whenDidSchemeAcquire: Option[LocalDate],
    companyNameRelatedShares: String,
    companySharesCrn: ConditionalYesNo[String, Crn],
    classOfShares: String,
    howManyShares: Int,
    identityType: Option[IdentityType],
    recipientName: Option[String],
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    sharesFromConnectedParty: Option[Boolean],
    costOfShares: Money,
    shareIndependentValue: Boolean,
    totalAssetValue: Option[Money],
    sharesTotalIncome: Money,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "checkYourAnswers.title",
        check = "sharesCYA.change.title",
        viewOnly = "sharesCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "checkYourAnswers.heading",
        check = Message("sharesCYA.change.heading", companyNameRelatedShares),
        viewOnly = Message("sharesCYA.viewOnly.heading", companyNameRelatedShares)
      ),
      description = Some(ParagraphMessage("sharesCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          index,
          schemeName,
          typeOfShare,
          holdShares,
          whenDidSchemeAcquire,
          companyNameRelatedShares,
          companySharesCrn,
          classOfShares,
          howManyShares,
          identityType,
          recipientName,
          recipientDetails,
          recipientReasonNoDetails,
          sharesFromConnectedParty,
          costOfShares,
          shareIndependentValue,
          totalAssetValue,
          sharesTotalIncome,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = routes.SharesCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "sharesCYA.viewOnly.title",
            heading = Message("sharesCYA.viewOnly.heading", companyNameRelatedShares),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                routes.SharesCYAController.onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                routes.SharesCYAController.onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def sections(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    typeOfShare: TypeOfShares,
    holdShares: SchemeHoldShare,
    whenDidSchemeAcquire: Option[LocalDate],
    companyNameRelatedShares: String,
    companySharesCrn: ConditionalYesNo[String, Crn],
    classOfShares: String,
    howManyShares: Int,
    identityType: Option[IdentityType],
    recipientName: Option[String],
    recipientDetails: Option[String],
    recipientReasonNoDetails: Option[String],
    sharesFromConnectedParty: Option[Boolean],
    costOfShares: Money,
    shareIndependentValue: Boolean,
    totalAssetValue: Option[Money],
    sharesTotalIncome: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    holdShares match {
      case Acquisition =>
        typeOfShares(
          srn,
          index,
          schemeName,
          typeOfShare,
          holdShares,
          mode
        ) ++ detailsOfTheTransaction(
          srn,
          index,
          schemeName: String,
          typeOfShare,
          holdShares,
          whenDidSchemeAcquire,
          companyNameRelatedShares,
          companySharesCrn,
          classOfShares,
          howManyShares,
          sharesFromConnectedParty,
          mode
        ) ++ detailsOfTheAcquisition(
          srn,
          index,
          identityType.get,
          recipientName.get,
          holdShares,
          companyNameRelatedShares,
          recipientDetails,
          recipientReasonNoDetails,
          sharesFromConnectedParty,
          mode
        ) ++ valueOfAndIncomeFrom(
          srn,
          index,
          schemeName,
          typeOfShare,
          holdShares,
          companyNameRelatedShares,
          costOfShares,
          shareIndependentValue,
          totalAssetValue,
          sharesTotalIncome,
          mode
        )

      case _ =>
        typeOfShares(
          srn,
          index,
          schemeName,
          typeOfShare,
          holdShares,
          mode
        ) ++ detailsOfTheTransaction(
          srn,
          index,
          schemeName,
          typeOfShare,
          holdShares,
          whenDidSchemeAcquire,
          companyNameRelatedShares,
          companySharesCrn,
          classOfShares,
          howManyShares,
          sharesFromConnectedParty,
          mode
        ) ++ valueOfAndIncomeFrom(
          srn,
          index,
          schemeName,
          typeOfShare,
          holdShares,
          companyNameRelatedShares,
          costOfShares,
          shareIndependentValue,
          totalAssetValue,
          sharesTotalIncome,
          mode
        )
    }

  private def typeOfShares(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    typeOfShare: TypeOfShares,
    holdShares: SchemeHoldShare,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("sharesCYA.section1.heading")),
        List(
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section1.typeOfShare"),
            typeOfShare match {
              case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
              case Unquoted => "sharesCYA.section1.Unquoted"
              case ConnectedParty => "sharesCYA.section1.ConnectedParty"
            }
          ).withAction(
            SummaryAction(
              "site.change",
              routes.TypeOfSharesHeldController.onPageLoad(srn, index, mode).url + "#typeOfShare"
            ).withVisuallyHiddenContent(
              ("sharesCYA.section1.typeOfShare.hidden", typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.Unquoted"
                case ConnectedParty => "sharesCYA.section1.ConnectedParty"
              })
            )
          )
        ) ++ List(
          CheckYourAnswersRowViewModel(
            Message(
              "sharesCYA.section1.holdShares",
              schemeName,
              typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.typeOfShares.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.typeOfShares.Unquoted"
                case ConnectedParty => "sharesCYA.section1.typeOfShares.ConnectedParty"
              }
            ),
            holdShares match {
              case Acquisition => "sharesCYA.section1.Acquisition"
              case Contribution => "sharesCYA.section1.Contribution"
              case Transfer => "sharesCYA.section1.Transfer"
            }
          ).withAction(
            SummaryAction(
              "site.change",
              routes.WhyDoesSchemeHoldSharesController.onPageLoad(srn, index, mode).url + "#holdShares"
            ).withVisuallyHiddenContent(
              Message(
                "sharesCYA.section1.holdShares.hidden",
                schemeName,
                typeOfShare match {
                  case SponsoringEmployer => "sharesCYA.section1.typeOfShares.SponsoringEmployer"
                  case Unquoted => "sharesCYA.section1.typeOfShares.Unquoted"
                  case ConnectedParty => "sharesCYA.section1.typeOfShares.ConnectedParty"
                },
                holdShares match {
                  case Acquisition => "sharesCYA.section1.Acquisition"
                  case Contribution => "sharesCYA.section1.Contribution"
                  case Transfer => "sharesCYA.section1.Transfer"
                }
              )
            )
          )
        )
      )
    )

  private def detailsOfTheTransaction(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    typeOfShare: TypeOfShares,
    holdShares: SchemeHoldShare,
    whenDidSchemeAcquire: Option[LocalDate],
    companyNameRelatedShares: String,
    companySharesCrn: ConditionalYesNo[String, Crn],
    classOfShares: String,
    howManyShares: Int,
    sharesFromConnectedParty: Option[Boolean],
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("sharesCYA.section2.heading")),
        whenDidSchemeAcquire.map { _ =>
          CheckYourAnswersRowViewModel(
            Message(
              "sharesCYA.section2.whenDidSchemeAcquire",
              schemeName,
              typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.typeOfShares.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.typeOfShares.Unquoted"
                case ConnectedParty => "sharesCYA.section1.typeOfShares.ConnectedParty"
              }
            ),
            s"${whenDidSchemeAcquire.get.show}"
          ).withAction(
            SummaryAction(
              "site.change",
              holdShares match {
                case holdShares if holdShares == Transfer =>
                  routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, mode).url
                case _ =>
                  routes.WhenDidSchemeAcquireSharesController
                    .onPageLoad(srn, index, mode)
                    .url + "#whenDidSchemeAcquire"
              }
            ).withVisuallyHiddenContent(
              Message(
                "sharesCYA.section2.whenDidSchemeAcquire.hidden",
                schemeName,
                typeOfShare match {
                  case SponsoringEmployer => "sharesCYA.section1.typeOfShares.SponsoringEmployer"
                  case Unquoted => "sharesCYA.section1.typeOfShares.Unquoted"
                  case ConnectedParty => "sharesCYA.section1.typeOfShares.ConnectedParty"
                }
              )
            )
          )
        }.toList ++ List(
          CheckYourAnswersRowViewModel(
            Message(
              "sharesCYA.section2.companyNameRelatedShares",
              typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.typeOfShares.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.typeOfShares.Unquoted"
                case ConnectedParty => "sharesCYA.section1.typeOfShares.ConnectedParty"
              }
            ),
            companyNameRelatedShares.show
          ).withAction(
            SummaryAction(
              "site.change",
              routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, mode).url + "#companyNameRelatedShares"
            ).withVisuallyHiddenContent(
              ("sharesCYA.section2.companyNameRelatedShares.hidden", typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.typeOfShares.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.typeOfShares.Unquoted"
                case ConnectedParty => "sharesCYA.section1.typeOfShares.ConnectedParty"
              })
            )
          ),
          companySharesCrn.value match {
            case Right(companySharesCrn) =>
              CheckYourAnswersRowViewModel(
                Message("sharesCYA.section2.companySharesCrn.yes", companyNameRelatedShares),
                companySharesCrn.value
              ).withAction(
                SummaryAction(
                  "site.change",
                  routes.SharesCompanyCrnController.onPageLoad(srn, index, mode).url + "#companySharesCrn"
                ).withVisuallyHiddenContent(
                  Message(
                    "sharesCYA.section2.companySharesCrn.yes.hidden",
                    companyNameRelatedShares,
                    companySharesCrn.value
                  )
                )
              )
            case Left(reason) =>
              CheckYourAnswersRowViewModel(
                Message("sharesCYA.section2.companySharesCrn.no", companyNameRelatedShares),
                reason
              ).withAction(
                SummaryAction(
                  "site.change",
                  routes.SharesCompanyCrnController.onPageLoad(srn, index, mode).url + "#companySharesCrn"
                ).withVisuallyHiddenContent(
                  Message("sharesCYA.section2.companySharesCrn.no.hidden", companyNameRelatedShares, reason)
                )
              )
          },
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section2.classOfShares", companyNameRelatedShares),
            classOfShares.show
          ).withAction(
            SummaryAction(
              "site.change",
              routes.ClassOfSharesController.onPageLoad(srn, index, mode).url + "#classOfShares"
            ).withVisuallyHiddenContent(("sharesCYA.section2.classOfShares.hidden", companyNameRelatedShares))
          ),
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section2.howManyShares", companyNameRelatedShares),
            howManyShares.show
          ).withAction(
            SummaryAction(
              "site.change",
              routes.HowManySharesController.onPageLoad(srn, index, mode).url + "#howManyShares"
            ).withVisuallyHiddenContent(("sharesCYA.section2.howManyShares.hidden", companyNameRelatedShares))
          )
        ) :?+ sharesFromConnectedParty.flatMap { connectedParty =>
          holdShares match {
            case SchemeHoldShare.Transfer =>
              Some(
                CheckYourAnswersRowViewModel(
                  Message("sharesCYA.sectionTransfer.sharesFromConnectedParty", companyNameRelatedShares),
                  if (connectedParty) "site.yes" else "site.no"
                ).withAction(
                  SummaryAction(
                    "site.change",
                    routes.SharesFromConnectedPartyController.onPageLoad(srn, index, mode).url
                  ).withVisuallyHiddenContent(
                    Message(
                      "sharesCYA.sectionTransfer.sharesFromConnectedParty.hidden",
                      companyNameRelatedShares,
                      if (connectedParty) "site.yes" else "site.no"
                    )
                  )
                )
              )
            case SchemeHoldShare.Contribution =>
              Some(
                CheckYourAnswersRowViewModel(
                  Message("sharesCYA.sectionContribution.sharesFromConnectedParty", companyNameRelatedShares),
                  if (connectedParty) "site.yes" else "site.no"
                ).withAction(
                  SummaryAction(
                    "site.change",
                    routes.SharesFromConnectedPartyController.onPageLoad(srn, index, mode).url
                  ).withVisuallyHiddenContent(
                    Message(
                      "sharesCYA.sectionContribution.sharesFromConnectedParty.hidden",
                      companyNameRelatedShares,
                      if (connectedParty) "site.yes" else "site.no"
                    )
                  )
                )
              )
            case _ => None
          }
        }
      )
    )

  private def detailsOfTheAcquisition(
    srn: Srn,
    index: Max5000,
    identityType: IdentityType,
    recipientName: String,
    holdShares: SchemeHoldShare,
    companyNameRelatedShares: String,
    optRecipientDetails: Option[String],
    optRecipientReasonNoDetails: Option[String],
    sharesFromConnectedParty: Option[Boolean],
    mode: Mode
  ): List[CheckYourAnswersSection] = {

    val sharesType = identityType match {
      case IdentityType.Individual => "landOrPropertySeller.identityType.pageContent"
      case IdentityType.UKCompany => "landOrPropertySeller.identityType.pageContent1"
      case IdentityType.UKPartnership => "landOrPropertySeller.identityType.pageContent2"
      case IdentityType.Other => "landOrPropertySeller.identityType.pageContent3"
    }

    val recipientNameUrl = identityType match {
      case IdentityType.Individual =>
        routes.IndividualNameOfSharesSellerController.onPageLoad(srn, index, mode).url
      case IdentityType.UKCompany => routes.CompanyNameOfSharesSellerController.onPageLoad(srn, index, mode).url
      case IdentityType.UKPartnership => routes.PartnershipNameOfSharesSellerController.onPageLoad(srn, index, mode).url
      case IdentityType.Other =>
        controllers.nonsipp.common.routes.OtherRecipientDetailsController
          .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
          .url
    }

    val (
      recipientDetailsKey,
      recipientDetailsUrl,
      recipientDetailsIdChangeHiddenKey,
      recipientDetailsNoIdChangeHiddenKey
    ): (Message, String, String, String) =
      identityType match {
        case IdentityType.Individual =>
          (
            Message("sharesCYA.section3.recipientDetails.nino", recipientName),
            routes.SharesIndividualSellerNINumberController.onPageLoad(srn, index, mode).url,
            "sharesCYA.section3.recipientDetails.nino.hidden",
            "sharesCYA.section3.recipientDetails.noNinoReason.hidden"
          )
        case IdentityType.UKCompany =>
          (
            Message("sharesCYA.section3.recipientDetails.crn", recipientName),
            controllers.nonsipp.common.routes.CompanyRecipientCrnController
              .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
              .url,
            "sharesCYA.section3.recipientDetails.crn.hidden",
            "sharesCYA.section3.recipientDetails.noCrnReason.hidden"
          )
        case IdentityType.UKPartnership =>
          (
            Message("sharesCYA.section3.recipientDetails.utr", recipientName),
            controllers.nonsipp.common.routes.PartnershipRecipientUtrController
              .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
              .url,
            "sharesCYA.section3.recipientDetails.utr.hidden",
            "sharesCYA.section3.recipientDetails.noUtrReason.hidden"
          )
        case IdentityType.Other =>
          (
            Message("sharesCYA.section3.recipientDetails.other", recipientName),
            controllers.nonsipp.common.routes.OtherRecipientDetailsController
              .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
              .url + "#other",
            "sharesCYA.section3.recipientDetails.other.hidden",
            ""
          )
      }

    val (recipientNoDetailsReasonKey, recipientNoDetailsUrl): (Message, String) = identityType match {
      case IdentityType.Individual =>
        Message("sharesCYA.section3.recipientDetails.noNinoReason", recipientName) ->
          routes.SharesIndividualSellerNINumberController.onPageLoad(srn, index, mode).url
      case IdentityType.UKCompany =>
        Message("sharesCYA.section3.recipientDetails.noCrnReason", recipientName) ->
          controllers.nonsipp.common.routes.CompanyRecipientCrnController
            .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
            .url
      case IdentityType.UKPartnership =>
        Message("sharesCYA.section3.recipientDetails.noUtrReason", recipientName) ->
          controllers.nonsipp.common.routes.PartnershipRecipientUtrController
            .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
            .url
      case IdentityType.Other =>
        Message("sharesCYA.section3.recipientDetails.other", recipientName) ->
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
            .url
    }

    List(
      CheckYourAnswersSection(
        Some(Heading2.medium("sharesCYA.section3.heading")),
        List(
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section3.whoWereSharesAcquired", companyNameRelatedShares),
            sharesType
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, index, mode, IdentitySubject.SharesSeller)
                .url
            ).withVisuallyHiddenContent(("sharesCYA.section3.whoWereSharesAcquired.hidden", companyNameRelatedShares))
          ),
          CheckYourAnswersRowViewModel("sharesCYA.section3.recipientName", recipientName)
            .withAction(
              SummaryAction("site.change", recipientNameUrl)
                .withVisuallyHiddenContent("sharesCYA.section3.recipientName.hidden")
            )
        ) :?+ optRecipientDetails.map { recipientDetails =>
          CheckYourAnswersRowViewModel(recipientDetailsKey, recipientDetails)
            .withAction(
              SummaryAction("site.change", recipientDetailsUrl)
                .withVisuallyHiddenContent(recipientDetailsIdChangeHiddenKey)
            )
        } :?+ optRecipientReasonNoDetails.map { recipientReasonNoDetails =>
          CheckYourAnswersRowViewModel(recipientNoDetailsReasonKey, recipientReasonNoDetails)
            .withAction(
              SummaryAction("site.change", recipientNoDetailsUrl)
                .withVisuallyHiddenContent(recipientDetailsNoIdChangeHiddenKey)
            )
        } :?+ sharesFromConnectedParty.flatMap { connectedParty =>
          holdShares match {
            case SchemeHoldShare.Acquisition =>
              Some(
                CheckYourAnswersRowViewModel(
                  Message("sharesCYA.section3.sharesFromConnectedParty", recipientName),
                  if (connectedParty) "site.yes" else "site.no"
                ).withAction(
                  SummaryAction(
                    "site.change",
                    routes.SharesFromConnectedPartyController.onPageLoad(srn, index, mode).url
                  ).withVisuallyHiddenContent(
                    Message(
                      "sharesCYA.section3.sharesFromConnectedParty.hidden",
                      recipientName,
                      if (connectedParty) "site.yes" else "site.no"
                    )
                  )
                )
              )
          }
        }
      )
    )
  }

  private def valueOfAndIncomeFrom(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    typeOfShare: TypeOfShares,
    holdShares: SchemeHoldShare,
    companyNameRelatedShares: String,
    costOfShares: Money,
    shareIndependentValue: Boolean,
    totalAssetValue: Option[Money],
    sharesTotalIncome: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        Some(Heading2.medium(("sharesCYA.section4.heading", companyNameRelatedShares))),
        List(
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section4.costOfShares"),
            s"£${costOfShares.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.CostOfSharesController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("sharesCYA.section4.costOfShares.hidden")
          ),
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section4.shareIndependentValue"),
            if (shareIndependentValue) "site.yes" else "site.no"
          ).withAction(
            SummaryAction(
              "site.change",
              routes.SharesIndependentValuationController.onPageLoad(srn, index, mode).url
            ).withVisuallyHiddenContent("sharesCYA.section4.shareIndependentValue.hidden")
          )
        ) :?+ totalAssetValue.map { totalAssetValue =>
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section4.totalAssetValue", schemeName),
            s"£${totalAssetValue.displayAs}"
          ).withAction(
            SummaryAction(
              "site.change",
              (typeOfShare, holdShares) match {
                case (typeOfShare, holdShares) if typeOfShare == SponsoringEmployer || holdShares == Acquisition =>
                  routes.TotalAssetValueController.onPageLoad(srn, index, mode).url
                case _ =>
                  routes.SharesTotalIncomeController.onPageLoad(srn, index, mode).url
              }
            ).withVisuallyHiddenContent(("sharesCYA.section4.totalAssetValue.hidden", schemeName))
          )
        } :+ CheckYourAnswersRowViewModel(
          Message("sharesCYA.section4.sharesTotalIncome"),
          s"£${sharesTotalIncome.displayAs}"
        ).withAction(
          SummaryAction(
            "site.change",
            routes.SharesTotalIncomeController.onPageLoad(srn, index, mode).url
          ).withVisuallyHiddenContent("sharesCYA.section4.sharesTotalIncome.hidden")
        )
      )
    )
}
