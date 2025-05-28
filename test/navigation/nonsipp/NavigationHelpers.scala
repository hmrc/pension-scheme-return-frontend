/*
 * Copyright 2025 HM Revenue & Customs
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

package navigation.nonsipp

import play.api.mvc.Call
import config.RefinedTypes.OneTo
import models.SchemeId.Srn
import models.{IdentitySubject, Mode}
import eu.timepit.refined.api.{Refined, Validate}

object RouteFnHelper {
  def refine[N](routeFn: (Srn, Int) => Call)(
    implicit v: Validate[Int, OneTo[N]]
  ): (Srn, Refined[Int, OneTo[N]]) => Call = { (srn: Srn, index: Refined[Int, OneTo[N]]) =>
    routeFn(srn, index.value)
  }
}

object RouteFnModeHelper {
  def refine[N](routeFn: (Srn, Int, Mode) => Call): (Srn, Refined[Int, OneTo[N]], Mode) => Call = {
    (srn: Srn, index: Refined[Int, OneTo[N]], mode: Mode) =>
      routeFn(srn, index.value, mode)
  }
}

object RouteFnDoubleModeHelper {
  def refine[N, M](
    routeFn: (Srn, Int, Int, Mode) => Call
  ): (Srn, Refined[Int, OneTo[N]], Refined[Int, OneTo[M]], Mode) => Call = {
    (srn: Srn, index: Refined[Int, OneTo[N]], index2: Refined[Int, OneTo[M]], mode: Mode) =>
      routeFn(srn, index.value, index2.value, mode)
  }
}

object RouteFnSubjectHelper {
  def refine[N](
    routeFn: (Srn, Int, IdentitySubject) => Call
  ): (Srn, Refined[Int, OneTo[N]], IdentitySubject) => Call = {
    (srn: Srn, index: Refined[Int, OneTo[N]], identitySubject: IdentitySubject) =>
      routeFn(srn, index.value, identitySubject)
  }
}

object RouteFnSubjectModeHelper {
  def refine[N](
    routeFn: (Srn, Int, Mode, IdentitySubject) => Call
  ): (Srn, Refined[Int, OneTo[N]], Mode, IdentitySubject) => Call = {
    (srn: Srn, index: Refined[Int, OneTo[N]], mode: Mode, identitySubject: IdentitySubject) =>
      routeFn(srn, index.value, mode, identitySubject)
  }
}
