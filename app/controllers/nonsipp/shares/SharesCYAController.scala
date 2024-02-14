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

package controllers.nonsipp.shares

import cats.implicits.toShow
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.shares.SharesCYAController._
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import models.SchemeId.Srn
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.{SchemeHoldShare, _}
import navigation.Navigator
import pages.nonsipp.common._
import pages.nonsipp.shares.{
  ClassOfSharesPage,
  CompanyNameOfSharesSellerPage,
  CompanyNameRelatedSharesPage,
  CostOfSharesPage,
  HowManySharesPage,
  IndividualNameOfSharesSellerPage,
  PartnershipShareSellerNamePage,
  SharesCYAPage,
  SharesCompanyCrnPage,
  SharesFromConnectedPartyPage,
  SharesIndependentValuationPage,
  SharesIndividualSellerNINumberPage,
  SharesTotalIncomePage,
  TotalAssetValuePage,
  TypeOfSharesHeldPage,
  WhenDidSchemeAcquireSharesPage,
  WhyDoesSchemeHoldSharesPage
}
import play.api.i18n._
import play.api.mvc._
import services.PsrSubmissionService
import utils.DateTimeUtils.localDateShow
import utils.ListUtils.ListOps
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class SharesCYAController @Inject()(
  override val messagesApi: MessagesApi,
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
    checkOrChange: CheckOrChange
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
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
              ViewModelParameters(
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
                checkOrChange
              )
            )
          )
        )
      ).merge

    }

  def onSubmit(srn: Srn, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService.submitPsrDetails(srn).map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) => Redirect(navigator.nextPage(SharesCYAPage(srn), NormalMode, request.userAnswers))
      }
    }
}

case class ViewModelParameters(
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
  checkOrChange: CheckOrChange
)
object SharesCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.checkOrChange.fold(check = "checkYourAnswers.title", change = "sharesCYA.change.title"),
      heading = parameters.checkOrChange.fold(
        check = "checkYourAnswers.heading",
        change = Message("sharesCYA.change.heading", parameters.companyNameRelatedShares)
      ),
      description = Some(ParagraphMessage("sharesCYA.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.index,
          parameters.schemeName,
          parameters.typeOfShare,
          parameters.holdShares,
          parameters.whenDidSchemeAcquire,
          parameters.companyNameRelatedShares,
          parameters.companySharesCrn,
          parameters.classOfShares,
          parameters.howManyShares,
          parameters.identityType,
          parameters.recipientName,
          parameters.recipientDetails,
          parameters.recipientReasonNoDetails,
          parameters.sharesFromConnectedParty,
          parameters.costOfShares,
          parameters.shareIndependentValue,
          parameters.totalAssetValue,
          parameters.sharesTotalIncome,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.checkOrChange.fold(check = "site.saveAndContinue", change = "site.continue"),
      onSubmit = routes.SharesCYAController.onSubmit(parameters.srn, parameters.checkOrChange)
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
            (typeOfShare) match {
              case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
              case Unquoted => "sharesCYA.section1.Unquoted"
              case ConnectedParty => "sharesCYA.section1.ConnectedParty"
            }
          ).withAction(
            SummaryAction(
              "",
              routes.TypeOfSharesHeldController.onPageLoad(srn, index, mode).url + "#typeOfShare"
            ).withVisuallyHiddenContent(
              "sharesCYA.section1.typeOfShare.hidden",
              typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.Unquoted"
                case ConnectedParty => "sharesCYA.section1.ConnectedParty"
              }
            )
          )
        ) ++ List(
          CheckYourAnswersRowViewModel(
            Message(
              "sharesCYA.section1.holdShares",
              schemeName,
              typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.Unquoted"
                case ConnectedParty => "sharesCYA.section1.ConnectedParty"
              }
            ),
            holdShares match {
              case Acquisition => "sharesCYA.section1.Acquisition"
              case Contribution => "sharesCYA.section1.Contribution"
              case Transfer => "sharesCYA.section1.Transfer"
            }
          ).withAction(
            SummaryAction(
              "",
              routes.WhyDoesSchemeHoldSharesController.onPageLoad(srn, index, mode).url + "#holdShares"
            ).withVisuallyHiddenContent(
              Message(
                "sharesCYA.section1.holdShares.hidden",
                schemeName,
                typeOfShare match {
                  case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                  case Unquoted => "sharesCYA.section1.Unquoted"
                  case ConnectedParty => "sharesCYA.section1.ConnectedParty"
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
                case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.Unquoted"
                case ConnectedParty => "sharesCYA.section1.ConnectedParty"
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
                  case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                  case Unquoted => "sharesCYA.section1.Unquoted"
                  case ConnectedParty => "sharesCYA.section1.ConnectedParty"
                }
              )
            )
          )
        }.toList ++ List(
          CheckYourAnswersRowViewModel(
            Message(
              "sharesCYA.section2.companyNameRelatedShares",
              typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.Unquoted"
                case ConnectedParty => "sharesCYA.section1.ConnectedParty"
              }
            ),
            companyNameRelatedShares.show
          ).withAction(
            SummaryAction(
              "site.change",
              routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, mode).url + "#companyNameRelatedShares"
            ).withVisuallyHiddenContent(
              "sharesCYA.section2.companyNameRelatedShares.hidden",
              typeOfShare match {
                case SponsoringEmployer => "sharesCYA.section1.SponsoringEmployer"
                case Unquoted => "sharesCYA.section1.Unquoted"
                case ConnectedParty => "sharesCYA.section1.ConnectedParty"
              }
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
            ).withVisuallyHiddenContent("sharesCYA.section2.classOfShares.hidden", companyNameRelatedShares)
          ),
          CheckYourAnswersRowViewModel(
            Message("sharesCYA.section2.howManyShares", companyNameRelatedShares),
            howManyShares.show
          ).withAction(
            SummaryAction(
              "site.change",
              routes.HowManySharesController.onPageLoad(srn, index, mode).url + "#howManyShares"
            ).withVisuallyHiddenContent("sharesCYA.section2.howManyShares.hidden", companyNameRelatedShares)
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
            ).withVisuallyHiddenContent("sharesCYA.section3.whoWereSharesAcquired.hidden", companyNameRelatedShares)
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
            ).withVisuallyHiddenContent("sharesCYA.section4.totalAssetValue.hidden", schemeName)
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
