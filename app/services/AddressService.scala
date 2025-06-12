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
import uk.gov.hmrc.http.HeaderCarrier
import models.{ALFAddressResponse, Address, LookupAddress}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.{Inject, Singleton}

@Singleton
class AddressService @Inject() (connector: AddressLookupConnector)(implicit
  ec: ExecutionContext
) {

  def postcodeLookup(postcode: String, filter: Option[String])(implicit hc: HeaderCarrier): Future[List[Address]] =
    connector
      .lookup(postcode, filter)
      .map(_.filter(_.address.lines.nonEmpty).map(addressFromALFAddress))
      .map(sortAddresses)

  private def sortAddresses(unsorted: List[Address]): List[Address] =
    unsorted.sortBy(s =>
      (
        s.street.getOrElse(""),
        s.houseNumber.getOrElse(Int.MaxValue),
        s.flatNumber.getOrElse(Int.MaxValue),
        s.flat.getOrElse("")
      )
    )

  private def getAddressSortingDetails(
    firstLine: String,
    secondLine: Option[String]
  ): (Option[String], Option[Int], Option[Int], Option[String]) = {
    val startsWithNumberRegex = """^\d.*[a-zA-Z ]$""".r
    val numberRegex = """\d+""".r
    val endsWithNumberRegex = """\d$""".r
    val containsNumberRegex = """\d""".r
    val firstLineSplit = firstLine.split(' ')
    val secondLineSplit = secondLine.getOrElse("").split(' ')

    (firstLine, secondLine) match {
      case (line1, None) if startsWithNumberRegex.matches(line1) =>
        // line1 starts with number, line2 not present
        val houseNumber = numberRegex.findFirstIn(line1).map(toIntOrMaxValue)
        val street = houseNumber.fold(line1)(_ => firstLineSplit.drop(1).mkString(" "))

        (Some(street), houseNumber, None, None)
      case (line1, None) if !startsWithNumberRegex.matches(line1) =>
        // line1 doesn't start with number, line2 not present
        (Some(line1), None, None, None)
      case (line1, Some(line2)) if startsWithNumberRegex.matches(line2) =>
        // line1 present, line2 starts with number
        val houseNumber = numberRegex.findFirstIn(line2).map(toIntOrMaxValue)
        val street = houseNumber.fold(line2)(_ => secondLineSplit.drop(1).mkString(" "))
        val flatNumber = endsWithNumberRegex.findFirstIn(line1).map(toIntOrMaxValue)
        val flat = flatNumber.fold(line1)(_ => firstLineSplit.dropRight(1).mkString(" "))

        (Some(street), houseNumber, flatNumber, Some(flat))
      case (line1, Some(line2))
          if firstLineSplit.length > 1 && startsWithNumberRegex.matches(line1) && !containsNumberRegex.matches(line2) =>
        // line1 present and ends with number, line2 doesn't contain number
        val houseNumber = numberRegex.findFirstIn(line1).map(toIntOrMaxValue)
        val street = houseNumber.fold(line1)(_ => firstLineSplit.drop(1).mkString(" "))

        (Some(street), houseNumber, None, None)
      case (line1, Some(line2)) if !startsWithNumberRegex.matches(line2) =>
        // line1 present, line2 doesn't start with number
        val houseNumber = numberRegex.findFirstIn(line1).map(toIntOrMaxValue)
        val street = line2

        (Some(street), houseNumber, None, None)
      case (_, _) =>
        (None, None, None, None)
    }
  }

  private def toIntOrMaxValue(s: String): Int =
    Try(s.toInt).getOrElse(Int.MaxValue)

  private def addressFromALFAddress(lookupResponse: ALFAddressResponse): Address = {
    val sortingDetails =
      getAddressSortingDetails(lookupResponse.address.firstLine.getOrElse(""), lookupResponse.address.secondLine)
    Address(
      lookupResponse.id,
      lookupResponse.address.firstLine.getOrElse(""),
      lookupResponse.address.secondLine,
      lookupResponse.address.thirdLine,
      lookupResponse.address.town,
      Some(lookupResponse.address.postcode),
      lookupResponse.address.country.name,
      lookupResponse.address.country.code,
      LookupAddress,
      sortingDetails._1,
      sortingDetails._2,
      sortingDetails._3,
      sortingDetails._4
    )
  }
}
