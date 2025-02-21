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

package models

import utils.{Diff, Transform}
import queries._
import models.UserAnswers.SensitiveJsObject
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import play.api.data.Form
import uk.gov.hmrc.crypto.json.JsonEncryption
import utils.JsonUtils.JsObjectOps
import viewmodels.models.Flag

import scala.util.{Failure, Success, Try}

import java.time.Instant

final case class UserAnswers(
  id: String,
  data: SensitiveJsObject = SensitiveJsObject(Json.obj()),
  lastUpdated: Instant = Instant.now
) { self =>

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data.decryptedValue).getOrElse(None)

  def get(path: JsPath)(implicit rds: Reads[JsValue]): Option[JsValue] =
    Reads.optionNoError(Reads.at(path)).reads(data.decryptedValue).getOrElse(None)

  def list[A](page: Gettable[List[A]])(implicit rds: Reads[A]): List[A] =
    get(page).getOrElse(Nil)

  def transformAndSet[A, B](
    page: Settable[B],
    value: A
  )(implicit writes: Writes[B], transform: Transform[A, B]): Try[UserAnswers] =
    set(page, transform.to(value))

  def map[A](page: Gettable[Map[String, A]])(implicit rds: Reads[A]): Map[String, A] =
    get(page).getOrElse(Map.empty)

  def fillForm[A, B](page: Gettable[B], form: Form[A])(implicit reads: Reads[B], transform: Transform[A, B]): Form[A] =
    get(page).fold(form)(b => form.fill(transform.from(b)))

  def set(page: Settable[Flag]): Try[UserAnswers] = set[Flag](page, Flag)

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] =
    page
      .cleanup(Some(value), self)
      .transform(
        _.setOnly(page, value),
        _ => setOnly(page, value)
      )

  def set(path: JsPath, value: JsValue): Try[UserAnswers] = {
    val updatedData = data.decryptedValue.setObject(path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = SensitiveJsObject(d))
      Success(updatedAnswers)
    }
  }

  def sameAs(other: UserAnswers, path: JsPath, toOmit: String*): Boolean = {
    val sanitise: JsValue => JsObject = json => json.as[JsObject].removeEmptyObjects().omit(toOmit: _*)
    this.get(path).map(sanitise) == other.get(path).map(sanitise)
  }

  /**
   * Returns a map of changed values between useranswers given a path
   * optional Ommited paths can be supplied
   */
  def diff(
    other: UserAnswers,
    path: JsPath,
    toOmit: String*
  ): Map[JsPath, (JsValue, JsValue)] =
    (this.get(path), other.get(path)) match {
      case (Some(a), Some(b)) =>
        Diff.json(
          a.as[JsObject].omit(toOmit: _*),
          b.as[JsObject].omit(toOmit: _*)
        )
      case (Some(a), None) =>
        Diff.json(
          a.as[JsObject].omit(toOmit: _*),
          JsObject.empty
        )
      case (None, Some(b)) =>
        Diff.json(
          JsObject.empty,
          b.as[JsObject].omit(toOmit: _*)
        )
      case _ => Map.empty
    }

  def compose(c: List[UserAnswers.Compose]): Try[UserAnswers] = c.foldLeft(Try(this))((ua, next) => next(ua))

  /**
   * Calls cleanup and then removes the page
   * @param page Page to remove
   * @return
   */
  def remove(page: Removable[_]): Try[UserAnswers] =
    page
      .cleanup(None, self)
      .transform(
        _.removeOnly(page),
        _ => removeOnly(page)
      )

  /**
   * Removes multiple pages without cleanup
   * @param pages List of pages to remove
   * @return
   */
  def removeOnlyMultiplePages(pages: List[Removable[_]]): Try[UserAnswers] =
    pages.foldLeft(Try(this))((ua, page) => ua.transform(_.removeOnly(page), _ => removeOnly(page)))

  /**
   * Removes multiple pages without cleanup
   * @param pages
   * @return
   */
  def removeOnly(pages: List[Removable[_]]): Try[UserAnswers] =
    pages.foldLeft(Try(this))((ua, page) => ua.flatMap(_.removeOnly(page)))

  /**
   * Removes multiple pages with cleanup. For each page calls cleanup and then removes the page.
   *
   * @param pages
   * @return
   */
  def remove(pages: List[Removable[_]]): Try[UserAnswers] =
    pages.foldLeft(Try(this))((ua, page) => ua.flatMap(_.remove(page)))

  def setWhen[A](bool: Boolean)(page: Settable[A], value: => A)(implicit writes: Writes[A]): Try[UserAnswers] =
    if (bool) setOnly(page, value) else Try(this)

  /**
   * Removes without cleanup if condition is true
   * @param bool condition
   * @param page page to remove
   * @return
   */
  def removeOnlyWhen(bool: Boolean)(page: Removable[_]*): Try[UserAnswers] =
    if (bool) page.foldLeft(Try(this))((ua, next) => ua.flatMap(_.removeOnly(next))) else Try(this)

  /**
   * Removes without cleanup if condition is true
   * @param bool condition as function
   * @param page page to remove
   * @return
   */
  def removeOnlyWhen(bool: UserAnswers => Boolean)(page: Removable[_]*): Try[UserAnswers] =
    if (bool(this)) page.foldLeft(Try(this))((ua, next) => ua.flatMap(_.removeOnly(next))) else Try(this)

  def when(
    get: UserAnswers => Option[Boolean]
  )(ifTrue: UserAnswers => Try[UserAnswers], ifFalse: UserAnswers => Try[UserAnswers]): Try[UserAnswers] =
    get(this) match {
      case Some(true) => ifTrue(this)
      case Some(false) => ifFalse(this)
      case None => Try(this)
    }

  def setOnly[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {
    val updatedData = data.decryptedValue.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.map(d => copy(data = SensitiveJsObject(d)))
  }

  def removeOnly[A](page: Removable[A]): Try[UserAnswers] = {
    val updatedData =
      data.decryptedValue.removeObject(page.path) match {
        case JsSuccess(jsValue, _) =>
          Success(jsValue)
        case JsError(_) =>
          Success(data.decryptedValue)
      }

    updatedData.map(d => copy(data = SensitiveJsObject(d)))
  }

  /**
   * - When soft deleting pages, the cleanup function is not called to stop accidentally hard deleting associated pages
   *   as cleanup pages use remove rather than softRemove.
   * */
  def softRemove[A: Reads: Writes](page: Gettable[A] with Settable[A] with Removable[A]): Try[UserAnswers] =
    get(page).fold(Try(this)) { value =>
      for {
        updated <- set(SoftRemovable.path ++ page.path, Json.toJson(value))
        removed <- updated.removeOnly(page)
      } yield removed
    }
}

object UserAnswers {

  type Compose = Try[UserAnswers] => Try[UserAnswers]

  def compose(
    fs: (UserAnswers => Try[UserAnswers])*
  ): UserAnswers => Try[UserAnswers] = ua => {
    fs.toList match {
      case Nil => Try(ua)
      case head :: tail =>
        tail.foldLeft(head(ua)) { (acc, curr) =>
          acc.flatMap(curr)
        }
    }
  }

  def get[A: Reads](page: Gettable[A]): UserAnswers => Option[A] = _.get(page)

  def set[A: Writes](page: Settable[A], value: A): UserAnswers => Try[UserAnswers] = _.set(page, value)

  def set(page: Settable[Flag]): UserAnswers => Try[UserAnswers] = set[Flag](page, Flag)

  def remove[A](page: Removable[A]): UserAnswers => Try[UserAnswers] = _.remove(page)

  def softRemove[A: Reads: Writes](page: SoftRemovable[A]): UserAnswers => Try[UserAnswers] = _.softRemove(page)

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

  object implicits {
    implicit class UserAnswersTryOps(ua: Try[UserAnswers]) {
      def set[A: Writes](page: Settable[A], value: A): Try[UserAnswers] = ua.flatMap(_.set(page, value))

      def set(page: Settable[Flag]): Try[UserAnswers] = ua.flatMap(_.set(page))

      def remove(page: Removable[_]): Try[UserAnswers] = ua.flatMap(_.remove(page))

      def setWhen[A: Writes](bool: Boolean)(page: Settable[A], value: A): Try[UserAnswers] =
        ua.flatMap(_.setWhen(bool)(page, value))
    }
  }
}
