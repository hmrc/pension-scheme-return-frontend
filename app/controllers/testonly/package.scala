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

package controllers

import models.NameDOB

import scala.util.Random

import java.time.LocalDate

package object testonly {
  private val firstNames = List(
    "Nathalia",
    "Kyro",
    "Jesse",
    "Mia",
    "Bradley",
    "Bonnie",
    "Wesley",
    "Alistair",
    "Fiona",
    "Victor"
  )

  private val lastNames = List(
    "Vazquez",
    "McMahon",
    "Davis",
    "Howell",
    "Willis",
    "Benjamin",
    "Mathews",
    "Sawyer",
    "Payne",
    "Gonzales"
  )

  def buildRandomNameDOB(): NameDOB = NameDOB(
    firstNames(Random.nextInt(firstNames.size)),
    lastNames(Random.nextInt(lastNames.size)),
    LocalDate.of(1990, 12, 12)
  )
}
