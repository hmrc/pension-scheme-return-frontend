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

package services

import play.api.test.FakeRequest
import play.api.mvc.AnyContentAsEmpty
import connectors.AddressLookupConnector
import controllers.TestValues
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import models._
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito.{reset, when}

import scala.io.Source
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AddressServiceSpec extends BaseSpec with TestValues {

  private val mockConnector = mock[AddressLookupConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private val service = new AddressService(mockConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  def readJsonFromFile(filePath: String): JsValue = {
    val resource = Source.fromURL(getClass.getResource(filePath))
    val json = Json.parse(resource.mkString)
    resource.close()
    json
  }

  "AddressService" - {
    "should return sorted addresses when real address details are used" in {
      val json = readJsonFromFile("/addressLookupResponse_B203LE.json")
      when(mockConnector.lookup(any(), any())(using any()))
        .thenReturn(
          Future.successful(
            json.as[List[ALFAddressResponse]]
          )
        )

      val addresses = service
        .postcodeLookup("any", None)
        .futureValue
        .map(s => (s.addressLine1, s.addressLine2, s.street, s.houseNumber, s.flatNumber, s.flat))

      addresses(0) mustEqual ("1 Haughton Road", None, Some("Haughton Road"), Some(1), None, None)
      addresses(1) mustEqual ("7 Haughton Road", None, Some("Haughton Road"), Some(7), None, None)
      addresses(2) mustEqual ("Flat 1", Some("9 Haughton Road"), Some("Haughton Road"), Some(9), Some(1), Some("Flat"))
      addresses(3) mustEqual ("Flat 2", Some("9 Haughton Road"), Some("Haughton Road"), Some(9), Some(2), Some("Flat"))
      addresses(4) mustEqual ("Flat 3", Some("9 Haughton Road"), Some("Haughton Road"), Some(9), Some(3), Some("Flat"))
      addresses(5) mustEqual ("11 Haughton Road", None, Some("Haughton Road"), Some(11), None, None)
      addresses(6) mustEqual ("11a", Some("Haughton Road"), Some("Haughton Road"), Some(11), None, None)
      addresses(7) mustEqual ("Flat 1, Wilmore House", Some("15 Haughton Road"), Some("Haughton Road"), Some(
        15
      ), None, Some(
        "Flat 1, Wilmore House"
      ))
      addresses(8) mustEqual ("Flat 2, Wilmore House", Some("15 Haughton Road"), Some("Haughton Road"), Some(
        15
      ), None, Some(
        "Flat 2, Wilmore House"
      ))
      addresses(9) mustEqual ("Flat 3, Wilmore House", Some("15 Haughton Road"), Some("Haughton Road"), Some(
        15
      ), None, Some(
        "Flat 3, Wilmore House"
      ))
      // skipped a few as they are repetitive
      addresses(15) mustEqual ("Flat 1", Some("17 Haughton Road"), Some("Haughton Road"), Some(17), Some(1), Some(
        "Flat"
      ))
      addresses(16) mustEqual ("Flat 2", Some("17 Haughton Road"), Some("Haughton Road"), Some(17), Some(2), Some(
        "Flat"
      ))
      addresses(17) mustEqual ("19 Haughton Road", None, Some("Haughton Road"), Some(19), None, None)
      // skipped the rest as they are repetitive
    }

    "should return expected address list with line1 only" in {
      when(mockConnector.lookup(any(), any())(using any()))
        .thenReturn(
          Future.successful(
            List(
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11a Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11-12 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Street")))
            )
          )
        )

      val addresses = service
        .postcodeLookup("any", None)
        .futureValue
        .map(s => (s.addressLine1, s.addressLine2, s.street, s.houseNumber, s.flatNumber, s.flat))

      addresses(0) mustEqual ("11 Street", None, Some("Street"), Some(11), None, None)
      addresses(1) mustEqual ("11a Street", None, Some("Street"), Some(11), None, None)
      addresses(2) mustEqual ("11-12 Street", None, Some("Street"), Some(11), None, None)
      addresses(3) mustEqual ("Street", None, Some("Street"), None, None, None)
    }
    "should return address with line2" in {
      when(mockConnector.lookup(any(), any())(using any()))
        .thenReturn(
          Future.successful(
            List(
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Flat 9", "11 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11a", "Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Flat 8", "11 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Flat 7", "11 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Flat 6", "11 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("1", "Street"))),
              alfAddressResponse.copy(
                address = alfAddressResponse.address.copy(lines = Seq("12 Street", "Some district"))
              ),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Housename", "Road"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("13 Road")))
            )
          )
        )

      val addresses = service
        .postcodeLookup("any", None)
        .futureValue
        .map(s => (s.addressLine1, s.addressLine2, s.street, s.houseNumber, s.flatNumber, s.flat))

      addresses(0) mustEqual ("13 Road", None, Some("Road"), Some(13), None, None)
      addresses(1) mustEqual ("Housename", Some("Road"), Some("Road"), None, None, None)
      addresses(2) mustEqual ("1", Some("Street"), Some("Street"), Some(1), None, None)
      addresses(3) mustEqual ("Flat 6", Some("11 Street"), Some("Street"), Some(11), Some(6), Some("Flat"))
      addresses(4) mustEqual ("Flat 7", Some("11 Street"), Some("Street"), Some(11), Some(7), Some("Flat"))
      addresses(5) mustEqual ("Flat 8", Some("11 Street"), Some("Street"), Some(11), Some(8), Some("Flat"))
      addresses(6) mustEqual ("Flat 9", Some("11 Street"), Some("Street"), Some(11), Some(9), Some("Flat"))
      addresses(7) mustEqual ("11a", Some("Street"), Some("Street"), Some(11), None, None)
      addresses(8) mustEqual ("12 Street", Some("Some district"), Some("Street"), Some(12), None, None)
    }
    "should omit address without any lines" in {
      when(mockConnector.lookup(any(), any())(using any()))
        .thenReturn(
          Future.successful(
            List(
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq.empty)),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11-12 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Street")))
            )
          )
        )

      val addresses = service
        .postcodeLookup("any", None)
        .futureValue
        .map(s => (s.addressLine1, s.addressLine2, s.street, s.houseNumber, s.flatNumber, s.flat))

      addresses.length mustBe 3
      addresses(0) mustEqual ("11 Street", None, Some("Street"), Some(11), None, None)
      addresses(1) mustEqual ("11-12 Street", None, Some("Street"), Some(11), None, None)
      addresses(2) mustEqual ("Street", None, Some("Street"), None, None, None)
    }

  }
}
