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

package models

import models.UserAnswers.SensitiveJsObject
import play.api.data.Form
import play.api.libs.json._
import queries.{Gettable, Settable}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.Transform

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
  id: String,
  data: SensitiveJsObject = SensitiveJsObject(Json.obj()),
  lastUpdated: Instant = Instant.now
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data.decryptedValue).getOrElse(None)

  def list[A](page: Gettable[List[A]])(implicit rds: Reads[A]): List[A] =
    get(page).getOrElse(Nil)

  def transformAndSet[A, B](
    page: Settable[B],
    value: A
  )(implicit writes: Writes[B], transform: Transform[A, B]): Try[UserAnswers] =
    set(page, transform.to(value))

  def map[A](page: Gettable[Map[String, A]])(implicit rds: Reads[A]): Map[String, A] =
    get(page).getOrElse(Map.empty)

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData =
      data.decryptedValue.setObject(page.path, Json.toJson(value)) match {
        case JsSuccess(jsValue, _) =>
          Success(jsValue)
        case JsError(errors) =>
          Failure(JsResultException(errors))
      }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = SensitiveJsObject(d))
      page.cleanup(Some(value), updatedAnswers)
    }
  }

  def fillForm[A, B](page: Gettable[B], form: Form[A])(implicit reads: Reads[B], transform: Transform[A, B]): Form[A] =
    get(page).fold(form)(b => form.fill(transform.from(b)))

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.decryptedValue.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_) =>
        Success(data.decryptedValue)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = SensitiveJsObject(d))
      page.cleanup(None, updatedAnswers)
    }
  }
}

object UserAnswers {

  case class SensitiveJsObject(override val decryptedValue: JsObject) extends Sensitive[JsObject]

  implicit def sensitiveJsObjectFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveJsObject] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveJsObject.apply)

  def reads(implicit crypto: Encrypter with Decrypter): Reads[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (__ \ "_id")
      .read[String]
      .and((__ \ "data").read[SensitiveJsObject])
      .and(
        (__ \ "lastUpdated")
          .read(MongoJavatimeFormats.instantFormat)
      )(UserAnswers.apply _)
  }

  def writes(implicit crypto: Encrypter with Decrypter): OWrites[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (__ \ "_id")
      .write[String]
      .and((__ \ "data").write[SensitiveJsObject])
      .and(
        (__ \ "lastUpdated")
          .write(MongoJavatimeFormats.instantFormat)
      )(unlift(UserAnswers.unapply))
  }

  implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[UserAnswers] = OFormat(reads, writes)
}
