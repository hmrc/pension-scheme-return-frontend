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

package org.scalatest

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.tools.ConcurrentDistributor
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent._
import ParallelSuite._
import play.api.Application
import play.api.test.PlayRunners

import scala.concurrent.ExecutionContext

object ParallelSuite {

  private val atomicThreadCounter = new AtomicInteger

  val threadFactory: ThreadFactory = new ThreadFactory {
    val defaultThreadFactory: ThreadFactory = Executors.defaultThreadFactory

    def newThread(runnable: Runnable): Thread = {
      val thread = defaultThreadFactory.newThread(runnable)
      val threadName = "ScalaTest-" + atomicThreadCounter.incrementAndGet()
      thread.setName(threadName)
      thread
    }
  }

  val executorService: ExecutorService =
    new ThreadPoolExecutor(20, 200, 60L, TimeUnit.SECONDS, new SynchronousQueue[Runnable], threadFactory)

  val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executorService)

  val appBuilder = new GuiceApplicationBuilder()

  val defaultActorSystem: ActorSystem = ActorSystem(
    name = "unit-tests",
    config = None,
    classLoader = None,
    defaultExecutionContext = Some(executionContext)
  )
}

abstract class ParallelSuite extends AnyFreeSpec with PlayRunners {

  override def shouldRunSequentially(app: Application): Boolean = false

  implicit val actorSystem: ActorSystem = defaultActorSystem

  implicit val mat: Materializer = Materializer.createMaterializer(actorSystem)

  override def run(testName: Option[String], args: Args): Status =
    super.run(
      testName,
      args.copy(
        distributor = Some(new ConcurrentDistributor(args, executorService))
      )
    )

  protected def applicationBuilder: GuiceApplicationBuilder =
    appBuilder
      .configure(
        "auditing.enabled" -> false,
        "metric.enabled" -> false
      )
      .overrides(
        bind[ExecutionContext].toInstance(executionContext)
      )
}
