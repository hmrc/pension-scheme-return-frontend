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

package controllers.actions

import play.api.mvc.{ActionBuilder, AnyContent}
import com.google.inject.Inject
import models.SchemeId.Srn
import models.{Mode, ViewOnlyMode}
import models.requests.DataRequest

class IdentifyAndRequireData @Inject()(
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  getDataToCompare: DataToCompareRetrievalActionProvider,
  requireData: DataRequiredAction
) {

  def apply(srn: Srn): ActionBuilder[DataRequest, AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData)

  def apply(srn: Srn, mode: Mode, year: String, current: Int, previous: Int): ActionBuilder[DataRequest, AnyContent] =
    if (mode == ViewOnlyMode) {
      identify
        .andThen(allowAccess(srn))
        .andThen(getDataToCompare(year, current, previous))
        .andThen(requireData)
    } else {
      identify
        .andThen(allowAccess(srn))
        .andThen(getData)
        .andThen(requireData)
    }
}
