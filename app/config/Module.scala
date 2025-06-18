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

package config

import play.api.inject.Binding
import navigation.nonsipp.NonSippNavigator
import controllers.actions._
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import navigation.{Navigator, RootNavigator}

import java.time.{Clock, ZoneOffset}

class Module extends play.api.inject.Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[?]] =
    Seq(
      bind[DataRetrievalAction].to(classOf[DataRetrievalActionImpl]).eagerly(),
      bind[DataRequiredAction].to(classOf[DataRequiredActionImpl]).eagerly(),
      bind[DataCreationAction].to(classOf[DataCreationActionImpl]).eagerly(),
      bind[Clock].toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC)),
      bind[Navigator].qualifiedWith(Names.named("root")).to(classOf[RootNavigator]).eagerly(),
      bind[Navigator].qualifiedWith(Names.named("non-sipp")).to(classOf[NonSippNavigator]).eagerly(),
      if (configuration.get[Boolean]("mongodb.encryption.enabled")) {
        bind[Crypto].to(classOf[CryptoImpl]).eagerly()
      } else {
        bind[Crypto].toInstance(Crypto.noop).eagerly()
      }
    )
}
