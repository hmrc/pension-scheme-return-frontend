package config

import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}

import java.security.SecureRandom
import java.util.Base64

object FakeCrypto extends Crypto {

  private val aesKey = {
    val aesKey = new Array[Byte](32)
    new SecureRandom().nextBytes(aesKey)
    Base64.getEncoder.encodeToString(aesKey)
  }

  override def getCrypto: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesGcmCrypto(aesKey)
}