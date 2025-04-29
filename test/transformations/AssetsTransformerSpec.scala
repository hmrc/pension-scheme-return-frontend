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

package transformations

import play.api.test.FakeRequest
import play.api.mvc.AnyContentAsEmpty
import pages.nonsipp.otherassetsheld.OtherAssetsHeldPage
import pages.nonsipp.landorproperty.{LandOrPropertyHeldPage, LandPropertyInUKPage}
import models.requests.psr._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import generators.ModelGenerators.allowedAccessRequestGen
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import models.requests.{AllowedAccessRequest, DataRequest}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.bonds.UnregulatedOrConnectedBondsHeldPage
import org.scalatest.freespec.AnyFreeSpec
import org.mockito.Mockito._
import transformations.AssetsTransformerSpec._
import org.scalatest.matchers.must.Matchers
import config.RefinedTypes.Max5000
import controllers.TestValues

import scala.util.Try

class AssetsTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with OptionValues
    with TestValues
    with BeforeAndAfterEach {

  private val index: Max5000 = refineMV(1)
  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val mockBondsTransformer = mock[BondsTransformer]
  private val mockOtherAssetsTransformer = mock[OtherAssetsTransformer]
  private val mockLandOrPropertyTransformer = mock[LandOrPropertyTransformer]
  private val mockBorrowingTransformer = mock[BorrowingTransformer]

  override def beforeEach(): Unit = {
    reset(mockLandOrPropertyTransformer)
    reset(mockBorrowingTransformer)
    reset(mockBondsTransformer)
    reset(mockOtherAssetsTransformer)
  }

  private val transformer = new AssetsTransformer(
    mockLandOrPropertyTransformer,
    mockBorrowingTransformer,
    mockBondsTransformer,
    mockOtherAssetsTransformer
  )

  "AssetsTransformer - To Etmp" - {
    "should return None when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn, defaultUserAnswers)
      result mustBe None
    }

    "should return Some Asset when userAnswer is not empty with proper answers" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandOrPropertyHeldPage(srn), false)
        .unsafeSet(MoneyBorrowedPage(srn), true)
        .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
        .unsafeSet(OtherAssetsHeldPage(srn), true)

      when(mockLandOrPropertyTransformer.transformToEtmp(any(), any(), any(), any())(any())).thenReturn(None)
      when(mockBorrowingTransformer.transformToEtmp(any(), any(), any())(any())).thenReturn(None)
      when(mockBondsTransformer.transformToEtmp(any(), any(), any(), any())(any())).thenReturn(None)
      when(mockOtherAssetsTransformer.transformToEtmp(any(), any(), any(), any())(any())).thenReturn(None)

      val result = transformer.transformToEtmp(srn, defaultUserAnswers)(DataRequest(allowedAccessRequest, userAnswers))
      result mustBe Some(Assets(optLandOrProperty = None, optBorrowing = None, optBonds = None, optOtherAssets = None))
    }

    "should return Some Asset when LandOrPropertyHeldPage is None but data is present - pre-population" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index), true)

      when(mockLandOrPropertyTransformer.transformToEtmp(any(), any(), any(), any())(any()))
        .thenReturn(Some(prePopulatedLandOrProperty))
      when(mockBorrowingTransformer.transformToEtmp(any(), any(), any())(any())).thenReturn(None)
      when(mockBondsTransformer.transformToEtmp(any(), any(), any(), any())(any())).thenReturn(None)
      when(mockOtherAssetsTransformer.transformToEtmp(any(), any(), any(), any())(any())).thenReturn(None)

      val result = transformer.transformToEtmp(srn, defaultUserAnswers)(DataRequest(allowedAccessRequest, userAnswers))
      result must not be None
    }
  }

  "AssetsTransformer - From Etmp" - {
    "should return data when only LandOrProperty data were found in etmp" in {

      when(mockLandOrPropertyTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, assetsWithLandOrProperty)

      result.fold(
        ex => fail(ex.getMessage + "\n" + ex.getStackTrace.mkString("\n")),
        userAnswers => {
          userAnswers mustBe defaultUserAnswers
          verify(mockLandOrPropertyTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockBorrowingTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        }
      )
    }

    "should return data when only Borrowing data were found in etmp" in {

      when(mockBorrowingTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, assetsWithBorrowing)

      result.fold(
        ex => fail(ex.getMessage + "\n" + ex.getStackTrace.mkString("\n")),
        userAnswers => {
          userAnswers mustBe defaultUserAnswers
          verify(mockLandOrPropertyTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBorrowingTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockBondsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        }
      )
    }

    "should return data when only Bonds data were found in etmp" in {

      when(mockBondsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, assetsWithBonds)

      result.fold(
        ex => fail(ex.getMessage + "\n" + ex.getStackTrace.mkString("\n")),
        userAnswers => {
          userAnswers mustBe defaultUserAnswers
          verify(mockLandOrPropertyTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBorrowingTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        }
      )
    }

    "should return data when only OtherAssets data were found in etmp" in {

      when(mockOtherAssetsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, assetsWithOtherAssets)

      result.fold(
        ex => fail(ex.getMessage + "\n" + ex.getStackTrace.mkString("\n")),
        userAnswers => {
          userAnswers mustBe defaultUserAnswers
          verify(mockLandOrPropertyTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBorrowingTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetsTransformer, times(1)).transformFromEtmp(any(), any(), any())
        }
      )
    }

  }

}

object AssetsTransformerSpec {
  val assetsWithLandOrProperty: Assets = Assets(
    optLandOrProperty = Some(
      LandOrProperty(
        Some("001"),
        optLandOrPropertyHeld = Some(true),
        optDisposeAnyLandOrProperty = Some(true),
        landOrPropertyTransactions = Seq.empty
      )
    ),
    optBorrowing = None,
    optBonds = None,
    optOtherAssets = None
  )

  val prePopulatedLandOrProperty: LandOrProperty = LandOrProperty(
    None,
    optLandOrPropertyHeld = None,
    optDisposeAnyLandOrProperty = None,
    landOrPropertyTransactions = Seq.empty // tests don't need to check these transactions in detail
  )

  val assetsWithBorrowing: Assets = Assets(
    optLandOrProperty = None,
    optBorrowing = Some(Borrowing(Some("001"), moneyWasBorrowed = true, moneyBorrowed = Seq.empty)),
    optBonds = None,
    optOtherAssets = None
  )

  val assetsWithBonds: Assets = Assets(
    optLandOrProperty = None,
    optBorrowing = None,
    optBonds = Some(
      Bonds(
        Some("001"),
        optBondsWereAdded = Some(true),
        optBondsWereDisposed = Some(true),
        bondTransactions = Seq.empty
      )
    ),
    optOtherAssets = None
  )

  val assetsWithOtherAssets: Assets = Assets(
    optLandOrProperty = None,
    optBorrowing = None,
    optBonds = None,
    optOtherAssets = Some(
      OtherAssets(
        Some("001"),
        optOtherAssetsWereHeld = Some(true),
        optOtherAssetsWereDisposed = Some(true),
        otherAssetTransactions = Seq.empty
      )
    )
  )
}
