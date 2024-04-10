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

import org.scalatest.freespec.AnyFreeSpecLike

trait ActsLikeSpec { _: AnyFreeSpecLike =>

  import Behaviours._

  /* ActWord has the same functionality as BehaveWord except it supports BehaviourTest
     for automatically running the test when passed to it, extending BehaveWord
     is not possible as it is a final class
   */
  class ActWord {

    def like(unit: Unit): Unit = ()

    def like(test: Behaviours): Unit = test.run()

    override def toString: String = "act"
  }

  val act = new ActWord

  sealed trait Behaviours {

    def run(): Unit
  }

  object Behaviours {

    case class BehaviourTest(
      name: String,
      test: () => Unit,
      beforeTest: () => Unit = () => (),
      afterTest: () => Unit = () => ()
    ) extends Behaviours {
      def withName(name: String): BehaviourTest = copy(name = name)

      def updateName(update: String => String): BehaviourTest = copy(name = update(name))

      def before(f: => Unit): BehaviourTest = copy(beforeTest = () => f)

      def after(f: => Unit): BehaviourTest = copy(afterTest = () => f)

      def run(): Unit =
        name in {
          beforeTest()
          test()
          afterTest()
        }
    }

    case class MultipleBehaviourTests(
      name: String,
      behaviours: List[BehaviourTest],
      beforeAllTests: () => Unit = () => (),
      afterAllTests: () => Unit = () => ()
    ) extends Behaviours {

      def withName(name: String): MultipleBehaviourTests = copy(name = name)

      def beforeAll(f: => Unit): MultipleBehaviourTests = copy(beforeAllTests = () => f)

      def afterAll(f: => Unit): MultipleBehaviourTests = copy(afterAllTests = () => f)

      def map(f: BehaviourTest => BehaviourTest): MultipleBehaviourTests =
        copy(behaviours = behaviours.map(f))

      def run(): Unit =
        name - {
          beforeAllTests()
          behaviours.foreach(_.run())
          afterAllTests()
        }
    }
  }

  implicit class BehaviourStringOps(name: String) {

    def hasBehaviour(behaviour: => Unit): BehaviourTest =
      BehaviourTest(name, () => behaviour)
  }
}
