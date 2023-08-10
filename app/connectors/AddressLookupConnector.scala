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

package connectors

import config.FrontendAppConfig
import connectors.AddressLookupConnector._
import models._
import play.api.http.Status._
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.{__, Format, JsError, JsObject, JsSuccess, Json, Reads}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.util.{Failure, Success, Try}

class AddressLookupConnector @Inject()(http: HttpClient, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) {

  private val initUrl = appConfig.addressLookupFrontend.baseUrl + "/api/v2/init"
  private val confirmedAddressUrl = appConfig.addressLookupFrontend.baseUrl + "/api/v2/confirmed"

  def init(continueUrl: String, isUkAddress: Boolean)(implicit hc: HeaderCarrier, messages: Messages): Future[String] =
    http
      .POST[JsObject, HttpResponse](
        initUrl,
        initConfig(continueUrl, isUkAddress),
        headers = List("Content-Type" -> "application/json")
      )
      .flatMap { response =>
        Future.fromTry(
          response.headers
            .get("location")
            .flatMap(_.headOption)
            .fold[Try[String]](Failure(new RuntimeException("location header expected")))(Success(_))
        )
      }

  def fetchAddress(id: String)(implicit hc: HeaderCarrier): Future[Either[ALFError, ALFAddress]] =
    http
      .GET[Either[ALFError, ALFAddress]](confirmedAddressUrl, queryParams = Seq("id" -> id))(
        ConfirmedAddressReads,
        implicitly,
        implicitly
      )

  private def initConfig(continueUrl: String, isUkAddress: Boolean)(implicit messages: Messages): JsObject = Json.obj(
    "version" -> 2,
    "options" -> Json.obj(
      "continueUrl" -> continueUrl,
      "phaseFeedbackLink" -> "/help/alpha",
      "ukMode" -> isUkAddress,
      "alphaPhase" -> true,
      "showPhaseBanner" -> true,
      "disableTranslations" -> true,
      "showBackButtons" -> true,
      "includeHMRCBranding" -> false,
      "pageHeadingStyle" -> "govuk-heading-l"
    ),
    "labels" -> Json.obj(
      "en" -> Json.obj(
        "appLevelLabels" -> Json.obj(
          "navTitle" -> "Pension Scheme Return"
        ),
        "countryPickerLabels" -> Json.obj(
          "title" -> messages("landOrPropertyAddressLookup.selectCountry.title"),
          "heading" -> messages("landOrPropertyAddressLookup.selectCountry.heading"),
          "countryLabel" -> messages("landOrPropertyAddressLookup.selectCountry.input"),
          "submitLabel" -> messages("site.saveAndContinue")
        ),
        "lookupPageLabels" -> Json.obj(
          "title" -> messages("landOrPropertyAddressLookup.postcodeLookup.title"),
          "heading" -> messages("landOrPropertyAddressLookup.postcodeLookup.heading"),
          "filterLabel" -> messages("landOrPropertyAddressLookup.postcodeLookup.filter.input"),
          "postcodeLabel" -> messages("landOrPropertyAddressLookup.postcodeLookup.postcode"),
          "submitLabel" -> messages("site.saveAndContinue"),
          "noResultsFoundMessage" -> messages("landOrPropertyAddressLookup.postcodeLookup.postcode.notFound"),
          "resultLimitExceededMessage" -> messages(
            "landOrPropertyAddressLookup.postcodeLookup.postcode.tooManyResults"
          ),
          "manualAddressLinkText" -> messages("landOrPropertyAddressLookup.postcodeLookup.manual")
        ),
        "selectPageLabels" -> Json.obj(
          "title" -> messages("landOrPropertyAddressLookup.chooseAddress.title"),
          "heading" -> messages("landOrPropertyAddressLookup.chooseAddress.heading"),
          "headingWithPostcode" -> messages("landOrPropertyAddressLookup.chooseAddress.proposal.heading"),
          "proposalListLabel" -> messages("landOrPropertyAddressLookup.chooseAddress.proposal.paragraph"),
          "submitLabel" -> messages("site.saveAndContinue"),
          "searchAgainLinkText" -> messages("landOrPropertyAddressLookup.chooseAddress.searchAgain.link"),
          "editAddressLinkText" -> messages("landOrPropertyAddressLookup.chooseAddress.editAddress.link")
        ),
        "editPageLabels" -> Json.obj(
          "title" -> messages("landOrPropertyAddressLookup.manualEntry.title"),
          "heading" -> messages("landOrPropertyAddressLookup.manualEntry.heading"),
          "organisationLabel" -> messages("landOrPropertyAddressLookup.manualEntry.input1"),
          "line1Label" -> messages("landOrPropertyAddressLookup.manualEntry.input2"),
          "line2Label" -> messages("landOrPropertyAddressLookup.manualEntry.input3"),
          "line3Label" -> messages("landOrPropertyAddressLookup.manualEntry.input4"),
          "townLabel" -> messages("landOrPropertyAddressLookup.manualEntry.input5"),
          "postcodeLabel" -> messages("landOrPropertyAddressLookup.manualEntry.input6"),
          "countryLabel" -> messages("landOrPropertyAddressLookup.manualEntry.input7"),
          "submitLabel" -> messages("site.saveAndContinue")
        ),
        "confirmPageLabels" -> Json.obj(
          "title" -> messages("landOrPropertyAddressLookup.confirmAddress.title"),
          "heading" -> messages("landOrPropertyAddressLookup.confirmAddress.heading"),
          "submitLabel" -> messages("site.saveAndContinue"),
          "changeLinkText" -> messages("landOrPropertyAddressLookup.confirmAddress.change.link")
        )
      )
    )
  )
}

object AddressLookupConnector {

  private implicit val countryReads: Reads[ALFCountry] = Json.reads[ALFCountry]

  private implicit val responseReads: Reads[ALFAddress] = Reads { json =>
    Reads.at[ALFAddress](__ \ "address")(Json.reads[ALFAddress]).reads(json)
  }

  implicit object ConfirmedAddressReads extends HttpReads[Either[ALFError, ALFAddress]] {

    def read(method: String, url: String, response: HttpResponse): Either[ALFError, ALFAddress] =
      response.status match {
        case OK =>
          response.json.validate[ALFAddress] match {
            case JsSuccess(address, _) => Right(address)
            case JsError(_) => Left(AddressMalformed)
          }
        case NOT_FOUND => Left(AddressNotFound)
        case _ => Left(UnexpectedFailure)
      }
  }
}
