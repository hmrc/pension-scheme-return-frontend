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

import connectors.AddressLookupConnector
import controllers.TestValues
import models.LookupAddress
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressServiceSpec extends BaseSpec with TestValues {

  private val mockConnector = mock[AddressLookupConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private val service = new AddressService(mockConnector)
  private val lookupAddress = address.copy(addressLine2 = None, addressLine3 = None, addressType = LookupAddress)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "AddressService" - {
    "should return sorted addresses" in {

      when(mockConnector.lookup(any(), any())(any()))
        .thenReturn(
          Future.successful(
            List(
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("1"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Flat 11"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Flat 2"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("111 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("11 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Flat 1"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Housename"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("2 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("10 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Studio 11"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("1 Street"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Studio 2"))),
              alfAddressResponse.copy(address = alfAddressResponse.address.copy(lines = Seq("Studio 1")))
            )
          )
        )

      val addresses = service.postcodeLookup("any", None).futureValue

      val expectedAddresses = List(
        lookupAddress.copy(
          addressLine1 = "1",
          addressLine1NumberPrefix = Some(1),
          addressLine1NumberPostfix = Some(1),
          addressLine1WithoutPostfix = ""
        ),
        lookupAddress.copy(
          addressLine1 = "1 Street",
          addressLine1NumberPrefix = Some(1),
          addressLine1NumberPostfix = None,
          addressLine1WithoutPostfix = "1 Street"
        ),
        lookupAddress.copy(
          addressLine1 = "2 Street",
          addressLine1NumberPrefix = Some(2),
          addressLine1NumberPostfix = None,
          addressLine1WithoutPostfix = "2 Street"
        ),
        lookupAddress.copy(
          addressLine1 = "10 Street",
          addressLine1NumberPrefix = Some(10),
          addressLine1NumberPostfix = None,
          addressLine1WithoutPostfix = "10 Street"
        ),
        lookupAddress.copy(
          addressLine1 = "11",
          addressLine1NumberPrefix = Some(11),
          addressLine1NumberPostfix = Some(11),
          addressLine1WithoutPostfix = ""
        ),
        lookupAddress.copy(
          addressLine1 = "11 Street",
          addressLine1NumberPrefix = Some(11),
          addressLine1NumberPostfix = None,
          addressLine1WithoutPostfix = "11 Street"
        ),
        lookupAddress.copy(
          addressLine1 = "111 Street",
          addressLine1NumberPrefix = Some(111),
          addressLine1NumberPostfix = None,
          addressLine1WithoutPostfix = "111 Street"
        ),
        lookupAddress.copy(
          addressLine1 = "Flat 1",
          addressLine1NumberPrefix = None,
          addressLine1NumberPostfix = Some(1),
          addressLine1WithoutPostfix = "Flat"
        ),
        lookupAddress.copy(
          addressLine1 = "Flat 2",
          addressLine1NumberPrefix = None,
          addressLine1NumberPostfix = Some(2),
          addressLine1WithoutPostfix = "Flat"
        ),
        lookupAddress.copy(
          addressLine1 = "Flat 11",
          addressLine1NumberPrefix = None,
          addressLine1NumberPostfix = Some(11),
          addressLine1WithoutPostfix = "Flat"
        ),
        lookupAddress.copy(
          addressLine1 = "Housename",
          addressLine1NumberPrefix = None,
          addressLine1NumberPostfix = None,
          addressLine1WithoutPostfix = "Housename"
        ),
        lookupAddress.copy(
          addressLine1 = "Studio 1",
          addressLine1NumberPrefix = None,
          addressLine1NumberPostfix = Some(1),
          addressLine1WithoutPostfix = "Studio"
        ),
        lookupAddress.copy(
          addressLine1 = "Studio 2",
          addressLine1NumberPrefix = None,
          addressLine1NumberPostfix = Some(2),
          addressLine1WithoutPostfix = "Studio"
        ),
        lookupAddress.copy(
          addressLine1 = "Studio 11",
          addressLine1NumberPrefix = None,
          addressLine1NumberPostfix = Some(11),
          addressLine1WithoutPostfix = "Studio"
        )
      )

      addresses mustEqual expectedAddresses
    }
  }
}
