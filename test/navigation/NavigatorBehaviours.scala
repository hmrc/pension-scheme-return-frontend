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

package navigation

import play.api.test.FakeRequest
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec
import play.api.mvc.{AnyContentAsEmpty, Call}
import pages.{Page, QuestionPage}
import controllers.TestValues
import models.SchemeId.Srn
import generators.IndexGen
import eu.timepit.refined.refineV
import models._
import models.requests.DataRequest
import eu.timepit.refined.api.{Refined, Validate}
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen
import org.scalatest.EitherValues
import play.api.libs.json.Writes

trait NavigatorBehaviours extends ScalaCheckPropertyChecks with EitherValues with TestValues { self: BaseSpec =>

  val navigator: Navigator

  protected trait AllModes {

    import Behaviours._

    implicit val req: DataRequest[AnyContentAsEmpty.type] =
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers)

    protected def navigateTo(mode: Mode)(
      page: Srn => Page,
      nextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers
    ): BehaviourTest =
      s"go from page to nextPage".hasBehaviour {
        forAll(srnGen) { srn =>
          navigator.nextPage(page(srn), mode, userAnswers(srn)) mustBe nextPage(srn, mode)
        }
      }

    protected def navigateToWithOldAndNewUserAnswers(mode: Mode)(
      page: Srn => Page,
      nextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers,
      oldUserAnswers: Srn => UserAnswers
    ): BehaviourTest =
      s"go from page to nextPage".hasBehaviour {
        forAll(srnGen) { srn =>
          navigator.nextPage(page(srn), mode, userAnswers(srn))(using
            DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, oldUserAnswers(srn))
          ) mustBe nextPage(srn, mode)
        }
      }

    protected def navigateToWithData[A: Writes](
      mode: Mode
    )(
      page: Srn => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers
    ): BehaviourTest =
      s"go from page to nextPage with data".hasBehaviour {
        forAll(srnGen, data) { (srn, data) =>
          val ua = userAnswers(srn).unsafeSet(page(srn), data)
          navigator.nextPage(page(srn), mode, ua) mustBe nextPage(srn, mode)
        }
      }

    protected def navigateFromListPage[A: Writes, Validator](mode: Mode)(
      listPage: Srn => Page,
      dataPage: (Srn, Refined[Int, Validator]) => QuestionPage[A],
      data: Gen[A],
      indexes: IndexGen[Validator],
      nextPage: (Srn, Int, Mode) => Call,
      maxDataNextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers
    )(implicit ev: Validate[Int, Validator]): MultipleBehaviourTests =
      MultipleBehaviourTests(
        "go from list page to next page",
        List(
          "when no data is added".hasBehaviour {
            forAll(srnGen, indexes.empty) { (srn, index) =>
              navigator.nextPage(listPage(srn), mode, userAnswers(srn)) mustBe nextPage(srn, index.value, mode)
            }
          },
          "when data is added".hasBehaviour {

            val dataGen = indexes.partial.flatMap { i =>
              Gen.listOfN(i.value, data).map((i, _))
            }

            forAll(srnGen, dataGen) { case (srn, (index, data)) =>
              val nextIndex = refineV(index.value + 1).value
              val ua =
                data.zipWithIndex.foldLeft(userAnswers(srn)) { case (acc, (curr, index)) =>
                  acc.unsafeSet(dataPage(srn, refineV(index + 1).value), curr)
                }

              navigator.nextPage(listPage(srn), mode, ua) mustBe nextPage(srn, nextIndex.value, mode)
            }
          },
          "when maximum amount of data has been added".hasBehaviour {

            val dataGen = indexes.full.flatMap { i =>
              Gen.listOfN(i.value, data)
            }

            forAll(srnGen, dataGen) { case (srn, data) =>
              val ua =
                data.zipWithIndex.foldLeft(userAnswers(srn)) { case (acc, (curr, index)) =>
                  acc.unsafeSet(dataPage(srn, refineV(index + 1).value), curr)
                }

              navigator.nextPage(listPage(srn), mode, ua) mustBe maxDataNextPage(srn, mode)
            }
          }
        )
      )
  }

  object normalmode extends AllModes {

    def navigateTo(
      page: Srn => Page,
      nextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(NormalMode)(page, nextPage, userAnswers)

    def navigateToWithIndex[A](
      index: Refined[Int, A],
      page: (Srn, Refined[Int, A]) => Page,
      nextPage: (Srn, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(NormalMode)(page(_, index), nextPage(_, index.value, _), userAnswers)

    def navigateToWithDoubleIndex[A, B](
      index: Refined[Int, A],
      secondIndex: Refined[Int, B],
      page: (Srn, Refined[Int, A], Refined[Int, B]) => Page,
      nextPage: (Srn, Int, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(NormalMode)(
        page(_, index, secondIndex),
        nextPage(_, index.value, secondIndex.value, _),
        userAnswers
      )

    def navigateToWithDoubleIndexAndData[A: Writes, I1, I2](
      index: Refined[Int, I1],
      secondIndex: Refined[Int, I2],
      page: (Srn, Refined[Int, I1], Refined[Int, I2]) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      navigateToWithData(NormalMode)(
        page(_, index, secondIndex),
        data,
        nextPage(_, index.value, secondIndex.value, _),
        userAnswers
      )

    def navigateToWithData[A: Writes](
      page: Srn => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(NormalMode)(page, data, nextPage, userAnswers)

    def navigateToWithDataAndIndex[A: Writes, B](
      index: Refined[Int, B],
      page: (Srn, Refined[Int, B]) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(NormalMode)(page(_, index), data, nextPage(_, index.value, _), userAnswers)

    def navigateToWithDataIndexAndSubject[A: Writes, B](
      index: Refined[Int, B],
      subject: IdentitySubject,
      page: (Srn, Refined[Int, B], IdentitySubject) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(NormalMode)(page(_, index, subject), data, nextPage(_, index.value, _), userAnswers)

    def navigateToWithDataIndexAndSubjects[A: Writes, B](
      index: Refined[Int, B],
      subject: IdentitySubject,
      page: (Srn, Refined[Int, B]) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Mode, IdentitySubject) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(NormalMode)(page(_, index), data, nextPage(_, index.value, _, subject), userAnswers)

    def navigateToWithDataIndexAndSubjectBoth[A: Writes, B](
      index: Refined[Int, B],
      subject: IdentitySubject,
      page: (Srn, Refined[Int, B], IdentitySubject) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Mode, IdentitySubject) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(NormalMode)(
        page(_, index, subject),
        data,
        nextPage(_, index.value, _, subject),
        userAnswers
      )

    def navigateToWithIndexAndSubject[A, B](
      index: Refined[Int, B],
      subject: IdentitySubject,
      page: (Srn, Refined[Int, B], IdentitySubject) => QuestionPage[A],
      nextPage: (Srn, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(NormalMode)(page(_, index, subject), nextPage(_, index.value, _), userAnswers)

    def navigateFromListPage[A: Writes, Validator](
      listPage: Srn => Page,
      dataPage: (Srn, Refined[Int, Validator]) => QuestionPage[A],
      data: Gen[A],
      indexes: IndexGen[Validator],
      nextPage: (Srn, Int, Mode) => Call,
      maxDataNextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    )(implicit ev: Validate[Int, Validator]): Behaviours.MultipleBehaviourTests =
      super.navigateFromListPage[A, Validator](NormalMode)(
        listPage,
        dataPage,
        data,
        indexes,
        nextPage,
        maxDataNextPage,
        userAnswers
      )
  }

  object checkmode extends AllModes {
    def navigateTo(
      page: Srn => Page,
      nextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(CheckMode)(page, nextPage, userAnswers)

    def navigateToWithData[A: Writes](
      page: Srn => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(CheckMode)(page, data, nextPage, userAnswers)

    def navigateFromListPage[A: Writes, Validator](
      listPage: Srn => Page,
      dataPage: (Srn, Refined[Int, Validator]) => QuestionPage[A],
      data: Gen[A],
      indexes: IndexGen[Validator],
      nextPage: (Srn, Int, Mode) => Call,
      maxDataNextPage: (Srn, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    )(implicit ev: Validate[Int, Validator]): Behaviours.MultipleBehaviourTests =
      super.navigateFromListPage[A, Validator](CheckMode)(
        listPage,
        dataPage,
        data,
        indexes,
        nextPage,
        maxDataNextPage,
        userAnswers
      )

    def navigateToWithIndex[A](
      index: Refined[Int, A],
      page: (Srn, Refined[Int, A]) => Page,
      nextPage: (Srn, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(CheckMode)(page(_, index), nextPage(_, index.value, _), userAnswers)

    def navigateToWithIndex[A](
      index: Refined[Int, A],
      page: (Srn, Refined[Int, A]) => Page,
      nextPage: (Srn, Int, Mode) => Call,
      oldUserAnswers: Srn => UserAnswers,
      userAnswers: Srn => UserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithOldAndNewUserAnswers(CheckMode)(
        page(_, index),
        nextPage(_, index.value, _),
        userAnswers,
        oldUserAnswers
      )

    def navigateToWithDoubleIndex[A, B](
      index: Refined[Int, A],
      secondIndex: Refined[Int, B],
      page: (Srn, Refined[Int, A], Refined[Int, B]) => Page,
      nextPage: (Srn, Int, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(CheckMode)(
        page(_, index, secondIndex),
        nextPage(_, index.value, secondIndex.value, _),
        userAnswers
      )

    def navigateToWithDoubleIndexAndData[A: Writes, I1, I2](
      index: Refined[Int, I1],
      secondIndex: Refined[Int, I2],
      page: (Srn, Refined[Int, I1], Refined[Int, I2]) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      navigateToWithData(CheckMode)(
        page(_, index, secondIndex),
        data,
        nextPage(_, index.value, secondIndex.value, _),
        userAnswers
      )

    def navigateToWithIndexAndSubject[A, B](
      index: Refined[Int, B],
      subject: IdentitySubject,
      page: (Srn, Refined[Int, B], IdentitySubject) => QuestionPage[A],
      nextPage: (Srn, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateTo(CheckMode)(page(_, index, subject), nextPage(_, index.value, _), userAnswers)

    def navigateToWithDataIndexAndSubject[A: Writes, B](
      index: Refined[Int, B],
      subject: IdentitySubject,
      page: (Srn, Refined[Int, B], IdentitySubject) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Mode) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(CheckMode)(page(_, index, subject), data, nextPage(_, index.value, _), userAnswers)

    def navigateToWithDataIndexAndSubjectBoth[A: Writes, B](
      index: Refined[Int, B],
      subject: IdentitySubject,
      page: (Srn, Refined[Int, B], IdentitySubject) => QuestionPage[A],
      data: Gen[A],
      nextPage: (Srn, Int, Mode, IdentitySubject) => Call,
      userAnswers: Srn => UserAnswers = _ => defaultUserAnswers
    ): Behaviours.BehaviourTest =
      super.navigateToWithData(CheckMode)(
        page(_, index, subject),
        data,
        nextPage(_, index.value, _, subject),
        userAnswers
      )
  }

}
