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

package config

import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import uk.gov.hmrc.crypto.{Crypted, PlainBytes, PlainText}
import controllers.ControllerBaseSpec
import play.api.inject.bind

class CryptoSpec extends ControllerBaseSpec {

  private val plain = "plain"
  private val plainText = PlainText(plain)
  private val plainBytes = PlainBytes(plain.getBytes)

  "encrypt/decrypt with noop instance" - {

    val app = GuiceApplicationBuilder()
      .overrides(
        List[GuiceableModule](
          bind[Crypto].toInstance(Crypto.noop).eagerly()
        ) ++ additionalBindings*
      )
      .configure("play.filters.csp.nonce.enabled" -> false)

    val crypto = app.injector().instanceOf[Crypto].getCrypto

    "text" in {
      val encrypted: Crypted = crypto.encrypt(plainText)
      val decrypted: PlainText = crypto.decrypt(encrypted)

      crypto.encrypt(plainText).value mustEqual plain
      decrypted.value mustEqual plain
    }

    "bytes" in {
      val encrypted: Crypted = crypto.encrypt(plainBytes)
      val decrypted: PlainBytes = crypto.decryptAsBytes(encrypted)

      crypto.encrypt(plainBytes).value mustEqual plain
      decrypted.value mustEqual plain.getBytes()
    }
  }

  "encrypt/decrypt with CryptoImpl instance" - {

    val app = GuiceApplicationBuilder()
      .overrides(
        List[GuiceableModule](
          bind[Crypto].to(classOf[CryptoImpl]).eagerly()
        ) ++ additionalBindings*
      )
      .configure("play.filters.csp.nonce.enabled" -> false)
    val crypto = app.injector().instanceOf[Crypto].getCrypto

    "text" in {
      val encrypted: Crypted = crypto.encrypt(plainText)

      (encrypted.value must not).equal(plain)
      crypto.decrypt(encrypted).value mustEqual plain

      val e: Exception = intercept[Exception](crypto.decrypt(Crypted("cant decrypt plain string")))
      e.getMessage mustEqual "Unable to decrypt value"
    }

    "bytes" in {
      val encrypted: Crypted = crypto.encrypt(plainBytes)
      val decrypted: PlainBytes = crypto.decryptAsBytes(encrypted)

      (crypto.encrypt(plainBytes).value must not).equal(plain)
      decrypted.value mustEqual plain.getBytes()
    }

  }
}
