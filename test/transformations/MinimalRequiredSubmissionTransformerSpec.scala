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

package transformations

import cats.data.NonEmptyList
import controllers.TestValues
import generators.ModelGenerators.allowedAccessRequestGen
import models.SchemeId.Srn
import models.SchemeMemberNumbers._
import models.requests.psr.{MinimalRequiredSubmission, ReportDetails, SchemeDesignatory}
import models.requests.{AllowedAccessRequest, DataRequest}
import models.{DateRange, MoneyInPeriod, NormalMode, SchemeMemberNumbers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify}
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import pages.nonsipp.{CheckReturnDatesPage, WhichTaxYearPage}
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.schemedesignatory.{
  ActiveBankAccountPage,
  FeesCommissionsWagesSalariesPage,
  HowManyMembersPage,
  ValueOfAssetsPage,
  WhyNoBankAccountPage
}
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import services.SchemeDateService
import utils.UserAnswersUtils.UserAnswersOps

class MinimalRequiredSubmissionTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    Mockito.reset(mockSchemeDateService)

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)
  private val mockReq = mock[DataRequest[AnyContent]]

  private val mockSchemeDateService = mock[SchemeDateService]

  private val transformer = new MinimalRequiredSubmissionTransformer(mockSchemeDateService)

  "Transform to ETMP" - {
    "should return None when userAnswer is empty" in {

      when(mockSchemeDateService.returnPeriods(any())(any())).thenReturn(Some(NonEmptyList.of(dateRange)))

      val result = transformer.transformToEtmp(srn)
      verify(mockSchemeDateService, times(1)).returnPeriods(any())(any())
      result mustBe None
    }

    "should return None when returnPeriods is None" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(HowManyMembersPage(srn, allowedAccessRequest.pensionSchemeId), SchemeMemberNumbers(2, 3, 4))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      when(mockSchemeDateService.returnPeriods(any())(any())).thenReturn(None)

      val result = transformer.transformToEtmp(srn)(request)
      verify(mockSchemeDateService, times(1)).returnPeriods(any())(any())
      result mustBe None
    }

    "should returned transformed object" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(WhyNoBankAccountPage(srn), "reasonForNoBankAccount")
        .unsafeSet(WhichTaxYearPage(srn), dateRange)
        .unsafeSet(ValueOfAssetsPage(srn, NormalMode), MoneyInPeriod(money, money))
        .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), money)
        .unsafeSet(HowManyMembersPage(srn, allowedAccessRequest.pensionSchemeId), SchemeMemberNumbers(2, 3, 4))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      when(mockSchemeDateService.returnPeriods(any())(any())).thenReturn(Some(NonEmptyList.of(dateRange)))

      val result = transformer.transformToEtmp(srn)(request)
      verify(mockSchemeDateService, times(1)).returnPeriods(any())(any())
      result mustBe Some(
        MinimalRequiredSubmission(
          ReportDetails(request.schemeDetails.pstr, dateRange.from, dateRange.to),
          NonEmptyList.of(dateRange.from -> dateRange.to),
          SchemeDesignatory(
            openBankAccount = false,
            Some("reasonForNoBankAccount"),
            2,
            3,
            4,
            Some(money.value),
            Some(money.value),
            None,
            None,
            Some(money.value)
          )
        )
      )
    }

  }

  "Transform from ETMP" - {
    "should transform minimal details" in {

      val userAnswers = defaultUserAnswers
      val minimalRequiredSubmission = MinimalRequiredSubmission(
        ReportDetails(request.schemeDetails.pstr, dateRange.from, dateRange.to),
        accountingPeriods,
        SchemeDesignatory(
          openBankAccount = true,
          //Some("reasonForNoBankAccount"),
          None,
          2,
          3,
          4,
          Some(money.value),
          Some(money.value),
          None,
          None,
          Some(money.value)
        )
      )

      val result = transformer.transformFromEtmp(
        userAnswers,
        Srn(allowedAccessRequest.schemeDetails.srn).get,
        allowedAccessRequest.pensionSchemeId,
        minimalRequiredSubmission
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(WhichTaxYearPage(srn)) mustBe Some(DateRange(dateRange.from, dateRange.to))
          userAnswers.get(CheckReturnDatesPage(srn)) mustBe Some(false)
          userAnswers.get(ActiveBankAccountPage(srn)) mustBe Some(true)
          userAnswers.get(WhyNoBankAccountPage(srn)) mustBe Some("reasonForNoBankAccount")
          userAnswers.get(HowManyMembersPage(srn, allowedAccessRequest.pensionSchemeId)) mustBe Some(
            SchemeMemberNumbers(2, 3, 4)
          )
          val aps = userAnswers.get(AccountingPeriods(srn))
          aps mustBe Some(accountingPeriods.toList.map(period => DateRange(period._1, period._2)))
        }
      )
    }
  }
}
