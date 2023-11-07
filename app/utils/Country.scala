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

package utils

import play.api.libs.json.{Json, OFormat}

import java.io.{File, FileInputStream, InputStream}
import scala.collection.Map
import scala.io.Source

case class Country(countryCode: String, country: String)

object Country {

  implicit val formats: OFormat[Country] = Json.format[Country]

  private val countries: List[Country] = {
    val jsonSchemaFile = new File(getClass.getClassLoader.getResource("country-code.json").toURI.getPath)
    val inputStream = new FileInputStream(jsonSchemaFile)
    val jsonFile = Json.parse(readStreamToString(inputStream))
    jsonFile.validate[List[Country]].get
  }

  private val countryCodesMap: Map[String, String] = {
    countries.map(country => (country.countryCode, country.country)).toMap
  }

  private val countriesMap: Map[String, String] = {
    countries.map(country => (country.country.toLowerCase, country.countryCode)).toMap
  }

  def getCountry(countryCode: String): Option[String] =
    countryCodesMap.get(countryCode)

  def getCountryCode(country: String): Option[String] =
    countriesMap.get(country.toLowerCase)

  private def readStreamToString(is: InputStream): String =
    try Source.fromInputStream(is).mkString
    finally is.close()
}
