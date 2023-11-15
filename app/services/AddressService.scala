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

package services

import connectors.AddressLookupConnector
import models.{ALFAddressResponse, Address, LookupAddress}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressService @Inject()(connector: AddressLookupConnector)(
  implicit ec: ExecutionContext
) {

  def postcodeLookup(postcode: String, filter: Option[String])(implicit hc: HeaderCarrier): Future[List[Address]] =
    connector.lookup(postcode, filter).map(_.map(addressFromALFAddress))

  private def addressFromALFAddress(lookupResponse: ALFAddressResponse): Address =
    Address(
      lookupResponse.id,
      lookupResponse.address.firstLine,
      lookupResponse.address.secondLine,
      lookupResponse.address.thirdLine,
      lookupResponse.address.town,
      Some(lookupResponse.address.postcode),
      lookupResponse.address.country.name,
      lookupResponse.address.country.code,
      LookupAddress
    )
}
