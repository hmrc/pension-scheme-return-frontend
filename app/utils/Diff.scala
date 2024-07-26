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
}
