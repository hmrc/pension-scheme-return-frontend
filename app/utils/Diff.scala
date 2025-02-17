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

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object Diff {

  def apply[T: TypeTag: ClassTag](a: T, b: T): List[String] = {
    val mirror = runtimeMirror(a.getClass.getClassLoader)
    val tpe = typeOf[T]

    val fields = tpe.decls.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }

    val instanceMirrorA = mirror.reflect(a)
    val instanceMirrorB = mirror.reflect(b)

    fields.toList.flatMap { field =>
      try {
        val fieldValueA = instanceMirrorA.reflectField(field).get
        val fieldValueB = instanceMirrorB.reflectField(field).get

        if (fieldValueA != fieldValueB) {
          List(s"Difference in field '${field.name}': '$fieldValueA' != '$fieldValueB'")
        } else {
          Nil
        }
      } catch {
        case ex: Exception =>
          List(s"Could not compare field '${field.name}': ${ex.getMessage}")
      }
    }
  }

  def json(obj1: JsValue, obj2: JsValue): Map[JsPath, (JsValue, JsValue)] = (obj1, obj2) match {
    case (o1: JsObject, o2: JsObject) =>
      val keys = o1.keys ++ o2.keys

      keys.flatMap { key =>
        (o1 \ key, o2 \ key) match {
          case (JsDefined(v1), JsDefined(v2)) if v1 != v2 =>
            val nestedDiff = json(v1, v2).map { case (p, diff) => (JsPath \ key ++ p) -> diff }
            if (nestedDiff.nonEmpty) nestedDiff else Map(JsPath \ key -> (v1, v2))

          case (JsDefined(v1), JsUndefined()) => Some(JsPath \ key -> (v1, JsNull))
          case (JsUndefined(), JsDefined(v2)) => Some(JsPath \ key -> (JsNull, v2))
          case _ => None
        }
      }.toMap

    case (JsArray(arr1), JsArray(arr2)) =>
      val maxLength = math.max(arr1.length, arr2.length)
      (0 until maxLength).flatMap { index =>
        (arr1.lift(index), arr2.lift(index)) match {
          case (Some(v1), Some(v2)) if v1 != v2 =>
            val nestedDiff = json(v1, v2).map { case (p, diff) => (JsPath(index) ++ p) -> diff }
            if (nestedDiff.nonEmpty) nestedDiff else Map(JsPath(index) -> (v1, v2))

          case (Some(v1), None) => Some(JsPath(index) -> (v1, JsNull))
          case (None, Some(v2)) => Some(JsPath(index) -> (JsNull, v2))
          case _ => None
        }
      }.toMap

    case _ if obj1 != obj2 => Map(JsPath -> (obj1, obj2))

    case _ => Map.empty
  }
}
