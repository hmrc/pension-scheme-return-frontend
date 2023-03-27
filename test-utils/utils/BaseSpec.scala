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

package utils

import akka.actor.ActorSystem
import akka.stream.Materializer
import generators.Generators
import models.ModelSerializers
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Span}
import org.scalatest.verbs.BehaveWord
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import java.net.URLEncoder
import scala.annotation.nowarn
import scala.reflect.ClassTag

abstract class BaseSpec
    extends AnyFreeSpec
    with ActsLikeSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with OptionValues
    with Generators
    with ModelSerializers {

  implicit val actorSystem: ActorSystem = ActorSystem("unit-tests")
  implicit val mat: Materializer = Materializer.createMaterializer(actorSystem)

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(500, Millis)), interval = scaled(Span(50, Millis)))

  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "metric.enabled" -> false
      )

  implicit def createMessages(implicit app: Application): Messages =
    app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def messages(key: String)(implicit m: Messages): String = m(key)

  protected def injected[A: ClassTag](implicit app: Application): A = app.injector.instanceOf[A]

  def runningApplication(block: Application => Unit): Unit =
    running(_ => applicationBuilder)(block)

  def urlEncode(input: String): String = URLEncoder.encode(input, "utf-8")

  @nowarn
  @deprecated("behave word has been replace with act word - behave.like becomes act.like", "0.44.0")
  override val behave: BehaveWord = new BehaveWord
}
