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

package pages.behaviours

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import queries.{Gettable, Removable, Settable}
import org.scalatest.matchers.must.Matchers
import pages.QuestionPage
import generators.Generators
import models.UserAnswers
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{OptionValues, TryValues}
import play.api.libs.json._

trait PageBehaviours
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues
    with TryValues {

  class BeRetrievable[A] {
    def apply[P <: QuestionPage[A]](genP: Gen[P])(implicit ev1: Arbitrary[A], ev2: Format[A]): Unit =
      "when being retrieved from UserAnswers" - {

        "and the question has not been answered" - {

          "must return None" in {

            val gen = for {
              page <- genP
              userAnswers <- arbitrary[UserAnswers]
            } yield (page, userAnswers.remove(page).success.value)

            forAll(gen) { case (page, userAnswers) =>
              userAnswers.get(page) must be(empty)
            }
          }
        }

        "and the question has been answered" - {

          "must return the saved value" in {

            val gen = for {
              page <- genP
              savedValue <- arbitrary[A]
              userAnswers <- arbitrary[UserAnswers]
            } yield (page, savedValue, userAnswers.set(page, savedValue).success.value)

            forAll(gen) { case (page, savedValue, userAnswers) =>
              userAnswers.get(page).value mustEqual savedValue
            }
          }
        }
      }

    def list(
      getter: Gen[Gettable[List[A]]],
      setter: Gen[Settable[A] & Removable[A]]
    )(implicit ev1: Arbitrary[A], ev2: Format[A]): Unit =
      "when being retrieved from UserAnswers" - {

        "and the question has not been answered" - {

          "must return None" in {

            val gen = for {
              getter <- getter
              setter <- setter
              userAnswers <- arbitrary[UserAnswers]
            } yield (getter, userAnswers.remove(setter).success.value)

            forAll(gen) { case (page, userAnswers) =>
              userAnswers.get(page) must be(empty)
            }
          }
        }

        "and the question has been answered" - {

          "must return the saved value" in {

            val gen = for {
              getter <- getter
              setter <- setter
              savedValue <- arbitrary[A]
              userAnswers <- arbitrary[UserAnswers]
            } yield (getter, savedValue, userAnswers.set(setter, savedValue).success.value)

            forAll(gen) { case (page, savedValue, userAnswers) =>
              userAnswers.get(page).value mustEqual List(savedValue)
            }
          }
        }
      }
  }

  class BeSettable[A] {
    def apply[P <: QuestionPage[A]](genP: Gen[P])(implicit ev1: Arbitrary[A], ev2: Format[A]): Unit =
      "must be able to be set on UserAnswers" in {

        val gen = for {
          page <- genP
          newValue <- arbitrary[A]
          userAnswers <- arbitrary[UserAnswers]
        } yield (page, newValue, userAnswers)

        forAll(gen) { case (page, newValue, userAnswers) =>
          val updatedAnswers = userAnswers.set(page, newValue).success.value
          updatedAnswers.get(page).value mustEqual newValue
        }
      }
  }

  class BeSettableWithIndex[A, Index] {
    def apply[P <: QuestionPage[A]](
      genP: Index => Gen[P]
    )(implicit ev1: Arbitrary[A], ev2: Arbitrary[Index], ev3: Format[A]): Unit =
      "must be able to be set on UserAnswers at any index" in {

        val gen = for {
          index <- arbitrary[Index]
          page <- genP(index)
          newValue <- arbitrary[A]
          userAnswers <- arbitrary[UserAnswers]
        } yield (page, newValue, userAnswers)

        forAll(gen) { case (page, newValue, userAnswers) =>
          val updatedAnswers = userAnswers.set(page, newValue).success.value
          updatedAnswers.get(page).value mustEqual newValue
        }
      }
  }

  class BeRemovable[A] {
    def apply[P <: QuestionPage[A]](genP: Gen[P])(implicit ev1: Arbitrary[A], ev2: Format[A]): Unit =
      "must be able to be removed from UserAnswers" in {

        val gen = for {
          page <- genP
          savedValue <- arbitrary[A]
          userAnswers <- arbitrary[UserAnswers]
        } yield (page, userAnswers.set(page, savedValue).success.value)

        forAll(gen) { case (page, userAnswers) =>
          val updatedAnswers = userAnswers.remove(page).success.value
          updatedAnswers.get(page) must be(empty)
        }
      }

    def list(
      getter: Gen[Gettable[List[A]]],
      setter: Gen[Settable[A]],
      remover: Gen[Removable[List[A]]]
    )(implicit ev1: Arbitrary[A], ev2: Format[A]): Unit =
      "must be able to be removed from UserAnswers" in {

        val gen = for {
          getter <- getter
          setter <- setter
          remover <- remover
          savedValue <- arbitrary[A]
          userAnswers <- arbitrary[UserAnswers]
        } yield (getter, remover, userAnswers.set(setter, savedValue).success.value)

        forAll(gen) { case (getter, remover, userAnswers) =>
          val updatedAnswers = userAnswers.remove(remover).success.value
          updatedAnswers.get(getter) must be(empty)
        }
      }
  }

  def beRetrievable[A]: BeRetrievable[A] = new BeRetrievable[A]

  def beSettable[A]: BeSettable[A] = new BeSettable[A]

  def beSettableWithIndex[A, Index]: BeSettableWithIndex[A, Index] = new BeSettableWithIndex[A, Index]

  def beRemovable[A]: BeRemovable[A] = new BeRemovable[A]
}
