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

package models

import play.api.mvc.Call

case class Pagination(
  currentPage: Int,
  pageSize: Int,
  totalSize: Int,
  call: Int => Call
) {
  val totalPages: Int = Math.ceil(totalSize.toDouble / pageSize.toDouble).toInt
  val pageStart: Int = if (currentPage == 1) 1 else pageSize * (currentPage - 1)
  val pageEnd: Int = if (currentPage == totalPages) totalSize else currentPage * pageSize
}
