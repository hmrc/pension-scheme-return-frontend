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

object Tuple2Utils {

//  implicit class Tuple2OptionOps[A, B](tup: (Option[A], Option[B])) {
//    def toEither: Either[A, B] = tup match {
//      case (Some(a), _) => Left(a)
//      case (_, Some(b)) => Right(b)
//    }
//  }

  implicit class Tuple2OptionOps[A, B](tup: (Option[A], Option[B])) {
    def toEither: Either[A, B] = tup match {
      case (Some(a), _) => Left(a)
      case (None, Some(b)) => Right(b)
      case (None, None) =>
        throw new NoSuchElementException("Neither Left nor Right value present in tuple")
    }
  }

}
