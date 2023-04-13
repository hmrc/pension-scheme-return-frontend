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

package navigation

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import generators.IndexGen
import models.SchemeId.Srn
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.{Page, QuestionPage}
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

trait NavigatorBehaviours extends ScalaCheckPropertyChecks with EitherValues { self: BaseSpec =>

  val navigator: Navigator

  protected trait AllModes {

    import Behaviours._

    protected def navigateTo(mode: Mode)(page: Srn => Page, nextPage: (Srn, Mode) => Call): BehaviourTest =
      s"go from page to nextPage".hasBehaviour {
        forAll(srnGen) { srn =>
          navigator.nextPage(page(srn), mode, UserAnswers("id")) mustBe nextPage(srn, mode)
        }
      }

    protected def navigateToWithData[A: Writes](
      mode: Mode
    )(page: Srn => QuestionPage[A], data: Gen[A], nextPage: (Srn, Mode) => Call): BehaviourTest =
      s"go from page to nextPage with data".hasBehaviour {
        forAll(srnGen, data) { (srn, data) =>
          val userAnswers = UserAnswers("id").unsafeSet(page(srn), data)
          navigator.nextPage(page(srn), mode, userAnswers) mustBe nextPage(srn, mode)
        }
      }

    protected def navigateFromListPage[A: Writes, Validator](mode: Mode)(
      listPage: Srn => Page,
      dataPage: (Srn, Refined[Int, Validator]) => QuestionPage[A],
      data: Gen[A],
      indexes: IndexGen[Validator],
      nextPage: (Srn, Refined[Int, Validator], Mode) => Call,
      maxDataNextPage: (Srn, Mode) => Call
    )(implicit ev: Validate[Int, Validator]): MultipleBehaviourTests =
      MultipleBehaviourTests(
        "go from list page to next page",
        List(
          "when no data is added".hasBehaviour {
            forAll(srnGen, indexes.empty) { (srn, index) =>
              navigator.nextPage(listPage(srn), mode, UserAnswers("id")) mustBe nextPage(srn, index, mode)
            }
          },
          "when data is added".hasBehaviour {

            val dataGen = indexes.partial.flatMap { i =>
              Gen.listOfN(i.value, data).map((i, _))
            }

            forAll(srnGen, dataGen) {
              case (srn, (index, data)) =>
                val nextIndex = refineV(index.value + 1).value
                val userAnswers =
                  data.zipWithIndex.foldLeft(UserAnswers("id")) {
                    case (acc, (curr, index)) =>
                      acc.unsafeSet(dataPage(srn, refineV(index + 1).value), curr)
                  }

                navigator.nextPage(listPage(srn), mode, userAnswers) mustBe nextPage(srn, nextIndex, mode)
            }
          },
          "when maximum amount of data has been added".hasBehaviour {

            val dataGen = indexes.full.flatMap { i =>
              Gen.listOfN(i.value, data)
            }

            forAll(srnGen, dataGen) {
              case (srn, data) =>
                val userAnswers =
                  data.zipWithIndex.foldLeft(UserAnswers("id")) {
                    case (acc, (curr, index)) =>
                      acc.unsafeSet(dataPage(srn, refineV(index + 1).value), curr)
                  }

                navigator.nextPage(listPage(srn), mode, userAnswers) mustBe maxDataNextPage(srn, mode)
            }
          }
        )
      )
  }

  object normalmode extends AllModes {
    def navigateTo(page: Srn => Page, nextPage: (Srn, Mode) => Call): Behaviours.BehaviourTest =
      super.navigateTo(NormalMode)(page, nextPage)

    def navigateToWithData[A: Writes](
      page: Srn => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Mode) => Call
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(NormalMode)(page, data, nextPage)

    def navigateFromListPage[A: Writes, Validator](
      listPage: Srn => Page,
      dataPage: (Srn, Refined[Int, Validator]) => QuestionPage[A],
      data: Gen[A],
      indexes: IndexGen[Validator],
      nextPage: (Srn, Refined[Int, Validator], Mode) => Call,
      maxDataNextPage: (Srn, Mode) => Call
    )(implicit ev: Validate[Int, Validator]): Behaviours.MultipleBehaviourTests =
      super.navigateFromListPage[A, Validator](NormalMode)(
        listPage,
        dataPage,
        data,
        indexes,
        nextPage,
        maxDataNextPage
      )
  }

  object checkmode extends AllModes {
    def navigateTo(page: Srn => Page, nextPage: (Srn, Mode) => Call): Behaviours.BehaviourTest =
      super.navigateTo(CheckMode)(page, nextPage)

    def navigateToWithData[A: Writes](
      page: Srn => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Mode) => Call
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(CheckMode)(page, data, nextPage)

    def navigateFromListPage[A: Writes, Validator](
      listPage: Srn => Page,
      dataPage: (Srn, Refined[Int, Validator]) => QuestionPage[A],
      data: Gen[A],
      indexes: IndexGen[Validator],
      nextPage: (Srn, Refined[Int, Validator], Mode) => Call,
      maxDataNextPage: (Srn, Mode) => Call
    )(implicit ev: Validate[Int, Validator]): Behaviours.MultipleBehaviourTests =
      super.navigateFromListPage[A, Validator](CheckMode)(
        listPage,
        dataPage,
        data,
        indexes,
        nextPage,
        maxDataNextPage
      )
  }

}
