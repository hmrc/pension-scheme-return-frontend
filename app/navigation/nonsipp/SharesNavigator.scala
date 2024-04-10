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

package navigation.nonsipp

import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import cats.implicits.toTraverseOps
import eu.timepit.refined.refineMV
import navigation.JourneyNavigator
import models._
import pages.nonsipp.common._
import models.IdentitySubject.SharesSeller
import pages.nonsipp.shares._
import play.api.mvc.Call
import config.Refined.Max5000
import pages.Page
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.SchemeId.Srn

object SharesNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ DidSchemeHoldAnySharesPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.shares.routes.WhatYouWillNeedSharesController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedSharesPage(srn) =>
      controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, refineMV(1), NormalMode)

    case TypeOfSharesHeldPage(srn, index) =>
      controllers.nonsipp.shares.routes.WhyDoesSchemeHoldSharesController.onPageLoad(srn, index, NormalMode)

    case WhyDoesSchemeHoldSharesPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {
        case Some(Acquisition) =>
          controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode)
        case Some(Contribution) =>
          controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode)
        case Some(Transfer) =>
          controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode)
        case _ => controllers.routes.UnauthorisedController.onPageLoad()
      }

    case WhenDidSchemeAcquireSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode)

    case CompanyNameRelatedSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesCompanyCrnController.onPageLoad(srn, index, NormalMode)

    case SharesCompanyCrnPage(srn, index) =>
      controllers.nonsipp.shares.routes.ClassOfSharesController.onPageLoad(srn, index, NormalMode)

    case ClassOfSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.HowManySharesController.onPageLoad(srn, index, NormalMode)

    case HowManySharesPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {

        case Some(Acquisition) =>
          controllers.nonsipp.common.routes.IdentityTypeController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)
        case _ =>
          userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
            case Some(Unquoted) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)
            case _ =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
          }

      }

    case IdentityTypePage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.SharesSeller)) match {
        case Some(IdentityType.Other) =>
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)
        case Some(IdentityType.Individual) =>
          controllers.nonsipp.shares.routes.IndividualNameOfSharesSellerController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.shares.routes.CompanyNameOfSharesSellerController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.shares.routes.PartnershipNameOfSharesSellerController
            .onPageLoad(srn, index, NormalMode)
        case _ =>
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case IndividualNameOfSharesSellerPage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesIndividualSellerNINumberController.onPageLoad(srn, index, NormalMode)

    case CompanyNameOfSharesSellerPage(srn, index) =>
      controllers.nonsipp.common.routes.CompanyRecipientCrnController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)

    case PartnershipShareSellerNamePage(srn, index) =>
      controllers.nonsipp.common.routes.PartnershipRecipientUtrController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)

    case SharesIndividualSellerNINumberPage(srn, index) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)

      }

    case CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
      }

    case PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
      }

    case OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
      }

    case SharesFromConnectedPartyPage(srn, index) =>
      controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)

    case CostOfSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesIndependentValuationController.onPageLoad(srn, index, NormalMode)

    case SharesIndependentValuationPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {
        case Some(Acquisition) =>
          userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
            case Some(SponsoringEmployer) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, NormalMode)
            case _ =>
              controllers.nonsipp.shares.routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode)
          }
        case _ =>
          controllers.nonsipp.shares.routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode)

      }

    case TotalAssetValuePage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode)

    case SharesTotalIncomePage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, NormalMode)

    case SharesCYAPage(srn) =>
      controllers.nonsipp.shares.routes.SharesListController.onPageLoad(srn, 1, NormalMode)

    case page @ SharesListPage(srn) =>
      userAnswers.get(page) match {
        case None => controllers.routes.JourneyRecoveryController.onPageLoad()
        case Some(true) =>
          (
            for {
              map <- userAnswers.get(SharesCompleted.all(srn)).getOrRecoverJourney
              indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
              _ <- if (indexes.size >= 5000) Left(controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
              else Right(())
              nextIndex <- findNextOpenIndex[Max5000.Refined](indexes).getOrRecoverJourney
            } yield controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, nextIndex, NormalMode)
          ).merge
        case Some(false) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case RemoveSharesPage(srn, _) =>
      if (userAnswers.map(CompanyNameRelatedSharesPages(srn)).isEmpty) {
        controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.shares.routes.SharesListController.onPageLoad(srn, page = 1, NormalMode)
      }

  }

  val checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = oldUserAnswers =>
    userAnswers => {

      case page @ TypeOfSharesHeldPage(srn, index) =>
        (oldUserAnswers.get(page), userAnswers.get(page)) match {
          // if unchanged answer is Unquoted, make sure shares from connected party is complete, otherwise go to CYA
          case (Some(Unquoted), Some(Unquoted)) =>
            if (userAnswers.get(SharesFromConnectedPartyPage(srn, index)).isEmpty) {
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, CheckMode)
            } else {
              controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
          // if unchanged answer is SponsoringEmployer and shares held type is Acquisition, make sure total asset value is complete, otherwise go to CYA
          case (Some(SponsoringEmployer), Some(SponsoringEmployer)) =>
            if (userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)).contains(Acquisition) &&
              userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty) {
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
            } else {
              controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
          // if unchanged answers is ConnectedParty, go to CYA
          case (Some(ConnectedParty), Some(ConnectedParty)) =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

          // if changed to SponsoringEmployer, go to "total asset value" if Acquisition, otherwise go to CYA
          case (Some(_), Some(SponsoringEmployer)) =>
            if (userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)).contains(Acquisition)) {
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
            } else {
              controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
          // if changed to Unquoted, go to "connected party"
          case (Some(_), Some(Unquoted)) =>
            controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, CheckMode)
          // if changed to ConnectedParty, go to CYA
          case (Some(_), Some(ConnectedParty)) =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          case _ => controllers.routes.UnauthorisedController.onPageLoad()
        }

      case WhyDoesSchemeHoldSharesPage(srn, index) =>
        (
          oldUserAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)),
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index))
        ) match {
          // if unchanged answer is Contribution, make sure when were shares acquired is complete, otherwise go to CYA
          case (Some(Contribution), Some(Contribution)) =>
            if (userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)).isEmpty) {
              controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, CheckMode)
            } else {
              controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
          // if unchanged answer is Acquisition, make sure
          // - when were shares acquired is complete
          // - a shares seller is complete
          // - total value of assets is complete if share type is a SponsoringEmployer
          // - connected party is complete if share type is Unquoted
          // otherwise go to CYA
          case (Some(Acquisition), Some(Acquisition)) =>
            if (userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)).isEmpty) {
              controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, CheckMode)
            } else {
              if (atLeastOneShareSellerComplete(srn, index, userAnswers)) {
                userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
                  case Some(SponsoringEmployer) if userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty =>
                    controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
                  case Some(Unquoted) if userAnswers.get(SharesFromConnectedPartyPage(srn, index)).isEmpty =>
                    controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                      .onPageLoad(srn, index, CheckMode)
                  case _ => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
                }
              } else {
                controllers.nonsipp.common.routes.IdentityTypeController
                  .onPageLoad(srn, index, CheckMode, IdentitySubject.SharesSeller)
              }
            }
          // if unchanged answer is Transfer, go to CYA
          case (Some(Transfer), Some(Transfer)) =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

          // if changed from Acquisition to Contribution, make sure "when were shares acquired" is complete
          // then go to connected party if type of shares held is unquoted, otherwise go to CYA
          case (Some(Acquisition), Some(Contribution)) =>
            if (userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)).isEmpty) {
              controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, CheckMode)
            } else {
              if (userAnswers.get(TypeOfSharesHeldPage(srn, index)).contains(Unquoted)) {
                controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, CheckMode)
              } else {
                controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
              }
            }
          // if changed from Acquisition to Transfer, go to "connected party" if type of shares held is unquoted, otherwise go to CYA
          case (Some(Acquisition), Some(Transfer)) =>
            if (userAnswers.get(TypeOfSharesHeldPage(srn, index)).contains(Unquoted)) {
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, CheckMode)
            } else {
              controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
          // if changed from Contribution to Acquisition, make sure when were shares acquired is complete, otherwise go to who were shares acquired from
          case (Some(Contribution), Some(Acquisition)) =>
            if (userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)).isEmpty) {
              controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, CheckMode)
            } else {
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, index, CheckMode, IdentitySubject.SharesSeller)
            }
          // if changed from Contribution to Transfer, go to CYA
          case (Some(Contribution), Some(Transfer)) =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          // if changed from Transfer to Acquisition or Contribution, go to when were shares acquired
          case (Some(Transfer), Some(Acquisition) | Some(Contribution)) =>
            controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, CheckMode)
          case _ => controllers.routes.UnauthorisedController.onPageLoad()
        }

      // Go to CYA unless any of the intermediate pages are not complete
      case WhenDidSchemeAcquireSharesPage(srn, index) =>
        userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {
          case Some(Acquisition) =>
            if (atLeastOneShareSellerComplete(srn, index, userAnswers)) {
              if (userAnswers.get(TypeOfSharesHeldPage(srn, index)).contains(SponsoringEmployer) &&
                userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty) {
                controllers.nonsipp.shares.routes.TotalAssetValueController
                  .onPageLoad(srn, index, CheckMode)
              } else {
                controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
              }
            } else {
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, index, CheckMode, IdentitySubject.SharesSeller)
            }
          case _ =>
            userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
              case Some(Unquoted) =>
                userAnswers.get(SharesFromConnectedPartyPage(srn, index)) match {
                  case Some(_) =>
                    controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
                  case _ =>
                    controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                      .onPageLoad(srn, index, CheckMode)
                }
              case _ => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
        }

      case CompanyNameRelatedSharesPage(srn, index) =>
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

      case SharesCompanyCrnPage(srn, index) =>
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

      case ClassOfSharesPage(srn, index) =>
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

      case HowManySharesPage(srn, index) =>
        userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {
          case Some(Acquisition) =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          case _ =>
            userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
              case Some(Unquoted) =>
                controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
              case _ =>
                controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
        }

      case IdentityTypePage(srn, index, IdentitySubject.SharesSeller) =>
        userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.SharesSeller)) match {
          case Some(IdentityType.Other) =>
            controllers.nonsipp.common.routes.OtherRecipientDetailsController
              .onPageLoad(srn, index, CheckMode, IdentitySubject.SharesSeller)
          case Some(IdentityType.Individual) =>
            controllers.nonsipp.shares.routes.IndividualNameOfSharesSellerController
              .onPageLoad(srn, index, CheckMode)
          case Some(IdentityType.UKCompany) =>
            controllers.nonsipp.shares.routes.CompanyNameOfSharesSellerController
              .onPageLoad(srn, index, CheckMode)
          case Some(IdentityType.UKPartnership) =>
            controllers.nonsipp.shares.routes.PartnershipNameOfSharesSellerController
              .onPageLoad(srn, index, CheckMode)
          case _ =>
            controllers.routes.UnauthorisedController.onPageLoad()
        }

      case IndividualNameOfSharesSellerPage(srn, index) =>
        userAnswers.get(SharesIndividualSellerNINumberPage(srn, index)) match {
          case None =>
            controllers.nonsipp.shares.routes.SharesIndividualSellerNINumberController.onPageLoad(srn, index, CheckMode)
          case _ =>
            userAnswers.get(SharesFromConnectedPartyPage(srn, index)) match {
              case None =>
                controllers.nonsipp.shares.routes.SharesIndividualSellerNINumberController
                  .onPageLoad(srn, index, CheckMode)
              case Some(_) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
        }

      case CompanyNameOfSharesSellerPage(srn, index) =>
        userAnswers.get(CompanyRecipientCrnPage(srn, index, SharesSeller)) match {
          case None =>
            controllers.nonsipp.common.routes.CompanyRecipientCrnController
              .onPageLoad(srn, index, CheckMode, SharesSeller)
          case _ =>
            userAnswers.get(SharesFromConnectedPartyPage(srn, index)) match {
              case None =>
                controllers.nonsipp.common.routes.CompanyRecipientCrnController
                  .onPageLoad(srn, index, CheckMode, SharesSeller)
              case Some(_) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
        }

      case PartnershipShareSellerNamePage(srn, index) =>
        userAnswers.get(PartnershipRecipientUtrPage(srn, index, SharesSeller)) match {
          case None =>
            controllers.nonsipp.common.routes.PartnershipRecipientUtrController
              .onPageLoad(srn, index, CheckMode, SharesSeller)
          case _ =>
            userAnswers.get(SharesFromConnectedPartyPage(srn, index)) match {
              case None =>
                controllers.nonsipp.common.routes.PartnershipRecipientUtrController
                  .onPageLoad(srn, index, CheckMode, SharesSeller)
              case Some(_) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
        }

      case OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller) =>
        userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
          case Some(Unquoted) if userAnswers.get(SharesFromConnectedPartyPage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
              .onPageLoad(srn, index, CheckMode)
          case Some(SponsoringEmployer) if userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
          case _ =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
        }

      case SharesIndividualSellerNINumberPage(srn, index) =>
        userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
          case Some(Unquoted) if userAnswers.get(SharesFromConnectedPartyPage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
              .onPageLoad(srn, index, CheckMode)
          case Some(SponsoringEmployer) if userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
          case _ =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

        }

      case CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller) =>
        userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
          case Some(Unquoted) if userAnswers.get(SharesFromConnectedPartyPage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
              .onPageLoad(srn, index, CheckMode)
          case Some(SponsoringEmployer) if userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
          case _ =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
        }

      case PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller) =>
        userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
          case Some(Unquoted) if userAnswers.get(SharesFromConnectedPartyPage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
              .onPageLoad(srn, index, CheckMode)
          case Some(SponsoringEmployer) if userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty =>
            controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
          case _ =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
        }

      case SharesFromConnectedPartyPage(srn, index) =>
        userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {
          case Some(Acquisition) =>
            userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
              case Some(SponsoringEmployer) if userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty =>
                controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
              case _ =>
                controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
            }
          case _ =>
            controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

        }

      case CostOfSharesPage(srn, index) =>
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

      case SharesIndependentValuationPage(srn, index) =>
        val nextPageIsTotalAssets =
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)).contains(Acquisition) &&
            userAnswers.get(TypeOfSharesHeldPage(srn, index)).contains(SponsoringEmployer) &&
            userAnswers.get(TotalAssetValuePage(srn, index)).isEmpty

        if (nextPageIsTotalAssets) {
          controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode)
        } else {
          controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
        }

      case TotalAssetValuePage(srn, index) =>
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

      case SharesTotalIncomePage(srn, index) =>
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)

    }

  def atLeastOneShareSellerComplete(srn: Srn, index: Max5000, userAnswers: UserAnswers): Boolean = {
    val individualCompleted = userAnswers.get(IndividualNameOfSharesSellerPage(srn, index)).nonEmpty &&
      userAnswers.get(SharesIndividualSellerNINumberPage(srn, index)).nonEmpty

    lazy val partnershipCompleted = userAnswers.get(PartnershipShareSellerNamePage(srn, index)).nonEmpty &&
      userAnswers.get(PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller)).nonEmpty

    lazy val companyCompleted = userAnswers.get(CompanyNameOfSharesSellerPage(srn, index)).nonEmpty &&
      userAnswers.get(CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller)).nonEmpty

    lazy val otherCompleted =
      userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller)).nonEmpty

    individualCompleted ||
    partnershipCompleted ||
    companyCompleted ||
    otherCompleted
  }
}
