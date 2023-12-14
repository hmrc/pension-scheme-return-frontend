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

import controllers.TestValues
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class MemberPaymentsTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  private val transformer = new MemberPaymentsTransformer()

  "MemberPaymentsTransformer - To Etmp" - {
    "should return empty List when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn, defaultUserAnswers)
      result mustBe None
    }
  }
}
