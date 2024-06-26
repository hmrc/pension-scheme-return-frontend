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

package utils

class ListUtilsSpec extends BaseSpec {

  "intersperse" - {
    "insert a separator between each element" in {
      val list = List("a", "b", "c")
      val expected = List("a", "-", "b", "-", "c")

      ListUtils.ListOps(list).intersperse("-") mustEqual expected
    }

    "insert a separator between every second element" in {
      val list = List("a", "b", "c", "d", "e", "f")
      val expected = List("a", "b", "-", "c", "d", "-", "e", "f")

      ListUtils.ListOps(list).intersperse("-", 2) mustEqual expected
    }
  }
}
