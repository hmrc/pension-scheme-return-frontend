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
import utils.BaseSpec
import play.api.mvc.AnyContentAsEmpty
import controllers.TestValues
import models.requests.{AllowedAccessRequest, DataRequest}
import models._
import models.EstablisherKind.Company

class PsrBaseServiceSpec extends BaseSpec with TestValues {

  private val service = new PsrBaseService {}

  "PsrBaseServiceSpec" - {
    val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] =
      allowedAccessRequestGen(FakeRequest()).sample.value
    "should return the establisher's name as a userName" in {

      implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(
        allowedAccessRequest.copy(
          schemeDetails = SchemeDetails(
            schemeName = schemeName,
            pstr = pstr,
            schemeStatus = schemeStatusGen.sample.value,
            schemeType = "schemeType",
            authorisingPSAID = Some("PSAID"),
            establishers = List(Establisher(name = "name", kind = Company))
          )
        ),
        defaultUserAnswers
      )

      service.schemeAdministratorOrPractitionerName mustBe "name"
    }

    "should return the Individual's fullName as a userName" in {

      implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(
        allowedAccessRequest.copy(
          schemeDetails = SchemeDetails(
            schemeName = schemeName,
            pstr = pstr,
            schemeStatus = schemeStatusGen.sample.value,
            schemeType = "schemeType",
            authorisingPSAID = Some("PSAID"),
            establishers = List.empty
          ),
          minimalDetails = MinimalDetails(
            email = "email",
            isPsaSuspended = true,
            organisationName = None,
            individualDetails =
              Some(IndividualDetails(firstName = "firstName", middleName = Some("middleName"), lastName = "lastName")),
            rlsFlag = true,
            deceasedFlag = true
          )
        ),
        defaultUserAnswers
      )

      service.schemeAdministratorOrPractitionerName mustBe "firstName middleName lastName"
    }

    "should return the OrganizationName as a userName" in {

      implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(
        allowedAccessRequest.copy(
          schemeDetails = SchemeDetails(
            schemeName = schemeName,
            pstr = pstr,
            schemeStatus = schemeStatusGen.sample.value,
            schemeType = "schemeType",
            authorisingPSAID = Some("PSAID"),
            establishers = List.empty
          ),
          minimalDetails = MinimalDetails(
            email = "email",
            isPsaSuspended = true,
            organisationName = Some("organisationName"),
            individualDetails = None,
            rlsFlag = true,
            deceasedFlag = true
          )
        ),
        defaultUserAnswers
      )

      service.schemeAdministratorOrPractitionerName mustBe "organisationName"
    }

    "should return Empty String as a userName" in {

      implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(
        allowedAccessRequest.copy(
          schemeDetails = SchemeDetails(
            schemeName = schemeName,
            pstr = pstr,
            schemeStatus = schemeStatusGen.sample.value,
            schemeType = "schemeType",
            authorisingPSAID = Some("PSAID"),
            establishers = List.empty
          ),
          minimalDetails = MinimalDetails(
            email = "email",
            isPsaSuspended = true,
            organisationName = None,
            individualDetails = None,
            rlsFlag = true,
            deceasedFlag = true
          )
        ),
        defaultUserAnswers
      )

      service.schemeAdministratorOrPractitionerName mustBe ""
    }
  }
}
