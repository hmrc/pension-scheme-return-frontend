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

import play.api.libs.json._

object JsonUtils {
  implicit class JsObjectOps(json: JsObject) {
    def +?(o: Option[JsObject]): JsObject = o.fold(json)(_ ++ json)
  }

  implicit class JsResultOps(result: JsResult[JsObject]) {

    /**
     * Prunes a path from a JsResult[JsObject]
     *
     * @param path
     *   The path to the value being pruned
     * @return
     *   A new json object with the path removed
     */
    def prune(path: JsPath): JsResult[JsObject] =
      result.flatMap(_.transform(path.prune(_)))
  }
}
