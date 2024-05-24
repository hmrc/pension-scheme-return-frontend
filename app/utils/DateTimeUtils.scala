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

import cats.Show

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

object DateTimeUtils {

  def formatHtml(localDate: LocalDate): String = {
    val formatter = DateTimeFormatter.ofPattern("d\u00A0MMMM\u00A0yyyy")
    localDate.format(formatter)
  }

  def formatHtml(localDateTime: LocalDateTime): String = {
    val formatter = DateTimeFormatter.ofPattern("d\u00A0MMMM\u00A0yyyy")
    localDateTime.format(formatter)
  }

  def formatReadable(localDateTime: LocalDateTime): String = {
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' h:mma")
    localDateTime.format(formatter)
  }

  implicit val localDateShow: Show[LocalDate] = d => formatHtml(d)
  implicit val localDateTimeShow: Show[LocalDateTime] = d => formatHtml(d)
}
