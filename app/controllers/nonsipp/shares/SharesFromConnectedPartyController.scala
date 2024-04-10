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

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import config.Refined.Max5000
import controllers.PSRController
import config.FrontendAppConfig
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import play.api.i18n.MessagesApi
import pages.nonsipp.shares._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SharesFromConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = SharesFromConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(WhyDoesSchemeHoldSharesPage(srn, index)).sync { schemeHoldShare =>
        if (!schemeHoldShare.name.equals("01")) {
          request
            .usingAnswer(CompanyNameRelatedSharesPage(srn, index))
            .sync { companyName =>
              Ok(
                view(
                  form.fromUserAnswers(SharesFromConnectedPartyPage(srn, index)),
                  SharesFromConnectedPartyController
                    .viewModel(srn, index, "", companyName, schemeHoldShare, config.urls.incomeTaxAct, mode)
                )
              )
            }

        } else {
          request.usingAnswer(WhyDoesSchemeHoldSharesPage(srn, index)).sync { schemeHoldShare =>
            recipientName(srn, index)
              .map { recipientName =>
                Ok(
                  view(
                    form.fromUserAnswers(SharesFromConnectedPartyPage(srn, index)),
                    SharesFromConnectedPartyController
                      .viewModel(
                        srn,
                        index,
                        recipientName,
                        "",
                        schemeHoldShare,
                        config.urls.incomeTaxAct,
                        mode
                      )
                  )
                )
              }
              .getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }

        }
      }

    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.usingAnswer(WhyDoesSchemeHoldSharesPage(srn, index)).async { schemeHoldShare =>
        request.usingAnswer(CompanyNameRelatedSharesPage(srn, index)).async { companyName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                recipientName(srn, index)
                  .map { recipientName =>
                    Future.successful(
                      BadRequest(
                        view(
                          formWithErrors,
                          SharesFromConnectedPartyController
                            .viewModel(
                              srn,
                              index,
                              recipientName,
                              companyName,
                              schemeHoldShare,
                              config.urls.incomeTaxAct,
                              mode
                            )
                        )
                      )
                    )
                  }
                  .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(SharesFromConnectedPartyPage(srn, index), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(
                  navigator.nextPage(SharesFromConnectedPartyPage(srn, index), mode, updatedAnswers)
                )
            )
        }
      }

  }

  private def recipientName(srn: Srn, index: Max5000)(implicit request: DataRequest[_]): Option[String] =
    request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.SharesSeller)).flatMap {
      case IdentityType.Individual => request.userAnswers.get(IndividualNameOfSharesSellerPage(srn, index))
      case IdentityType.UKCompany => request.userAnswers.get(CompanyNameOfSharesSellerPage(srn, index))
      case IdentityType.UKPartnership => request.userAnswers.get(PartnershipShareSellerNamePage(srn, index))
      case IdentityType.Other =>
        request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller)).map(_.name)
      case _ => None

    }

}

object SharesFromConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "sharesFromConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    companyName: String,
    schemeHoldShare: SchemeHoldShare,
    incomeTaxAct: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    if (schemeHoldShare.name.equals("01")) {

      YesNoPageViewModel(
        Message("sharesFromConnectedParty.acquisitionTitle"),
        Message("sharesFromConnectedParty.acquisitionHeading", individualName),
        Option(
          FurtherDetailsViewModel(
            Message("sharesFromConnectedParty.acquisitionContent"),
            ParagraphMessage("sharesFromConnectedParty.acquisitionParagraph1") ++
              ParagraphMessage("sharesFromConnectedParty.acquisitionParagraph2") ++
              ParagraphMessage("sharesFromConnectedParty.acquisitionParagraph3") ++
              ListMessage(
                ListType.Bullet,
                "sharesFromConnectedParty.acquisitionBullet1",
                "sharesFromConnectedParty.acquisitionBullet2"
              ) ++
              ParagraphMessage(
                "sharesFromConnectedParty.acquisitionParagraph4",
                LinkMessage(
                  "sharesFromConnectedParty.acquisitionParagraph4.link",
                  incomeTaxAct,
                  Map("rel" -> "noreferrer noopener", "target" -> "_blank")
                )
              )
          )
        ),
        controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onSubmit(srn, index, mode)
      )

    } else {
      (schemeHoldShare.name.equals("02"))
      (schemeHoldShare.name.equals("03"))
      YesNoPageViewModel(
        Message("sharesFromConnectedParty.title1"),
        Message("sharesFromConnectedParty.heading1", companyName),
        Option(
          FurtherDetailsViewModel(
            Message("sharesFromConnectedParty.content1"),
            ParagraphMessage("sharesFromConnectedParty.paragraph1") ++
              ParagraphMessage("sharesFromConnectedParty.paragraph2") ++
              ParagraphMessage("sharesFromConnectedParty.paragraph3") ++
              ListMessage(
                ListType.Bullet,
                "sharesFromConnectedParty.bullet1",
                "sharesFromConnectedParty.bullet2"
              ) ++
              ParagraphMessage(
                "sharesFromConnectedParty.paragraph4",
                LinkMessage(
                  "sharesFromConnectedParty.paragraph4.link",
                  incomeTaxAct,
                  Map("rel" -> "noreferrer noopener", "target" -> "_blank")
                )
              )
          )
        ),
        controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onSubmit(srn, index, mode)
      )
    }
}
