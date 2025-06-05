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

import com.google.inject.ImplementedBy
import uk.gov.hmrc.crypto._
import play.api.Configuration

import java.nio.charset.StandardCharsets
import javax.inject.Inject

@ImplementedBy(classOf[CryptoImpl])
trait Crypto {
  def getCrypto: Encrypter with Decrypter
}

class CryptoImpl @Inject() (config: Configuration) extends Crypto {
  override def getCrypto: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("mongodb.encryption", config.underlying)
}

object Crypto {

  def noop: Crypto = new Crypto {
    override def getCrypto: Encrypter with Decrypter = new Encrypter with Decrypter {
      override def encrypt(plain: PlainContent): Crypted = plain match {
        case PlainText(value) => Crypted(value)
        case PlainBytes(value) => Crypted(new String(value))
      }

      override def decrypt(reversiblyEncrypted: Crypted): PlainText = PlainText(reversiblyEncrypted.value)

      override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes =
        PlainBytes(reversiblyEncrypted.value.getBytes(StandardCharsets.UTF_8))
    }
  }
}
