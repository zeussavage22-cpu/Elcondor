package com.elcondor.cloner.core

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * The key every clone is signed with. It is created once and stored in a PKCS#12 file, so later
 * clones of the same app can be installed as updates of earlier ones.
 */
class SigningIdentity(val privateKey: PrivateKey, val certificate: X509Certificate) {

    companion object {
        private const val ALIAS = "clone"

        fun loadOrCreate(file: File, password: CharArray): SigningIdentity {
            if (file.isFile) {
                runCatching { return load(file, password) }
            }
            val identity = generate()
            val ks = KeyStore.getInstance("PKCS12")
            ks.load(null, null)
            ks.setKeyEntry(ALIAS, identity.privateKey, password, arrayOf(identity.certificate))
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".tmp")
            tmp.outputStream().use { ks.store(it, password) }
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
            return identity
        }

        private fun load(file: File, password: CharArray): SigningIdentity {
            val ks = KeyStore.getInstance("PKCS12")
            file.inputStream().use { ks.load(it, password) }
            val key = ks.getKey(ALIAS, password) as PrivateKey
            val cert = ks.getCertificate(ALIAS) as X509Certificate
            return SigningIdentity(key, cert)
        }

        fun generate(): SigningIdentity {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048, SecureRandom())
            val pair = kpg.generateKeyPair()
            val subject = X500Name("CN=Elcondor Cloner, O=Elcondor")
            val now = System.currentTimeMillis()
            val builder = JcaX509v3CertificateBuilder(
                subject,
                BigInteger(63, SecureRandom()).add(BigInteger.ONE),
                Date(now - TimeUnit.DAYS.toMillis(1)),
                Date(now + TimeUnit.DAYS.toMillis(365L * 30)),
                subject,
                pair.public,
            )
            val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
            val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))
            return SigningIdentity(pair.private, cert)
        }
    }
}
