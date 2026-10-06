package com.xraiassistant.di

import okhttp3.internal.tls.OkHostnameVerifier
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.KeyStore

/**
 * Guards against the trust-all TLS setup coming back: the shared client must use
 * the platform's certificate and hostname checks, and must never log bodies.
 */
class NetworkModuleTest {

    private val client = NetworkModule.provideOkHttpClient()

    @Test
    fun `hostname verification is the OkHttp default`() {
        assertSame(OkHostnameVerifier, client.hostnameVerifier)
    }

    @Test
    fun `certificates are checked by the platform trust manager`() {
        val platform = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null as KeyStore?) }
            .trustManagers.filterIsInstance<X509TrustManager>().first()
        assertTrue(
            "client trust manager ${client.x509TrustManager?.javaClass} is not the platform's",
            client.x509TrustManager?.javaClass == platform.javaClass
        )
        assertTrue("a trust manager that accepts no issuers trusts everything",
            client.x509TrustManager!!.acceptedIssuers.isNotEmpty())
    }

    @Test
    fun `request and response bodies are never logged`() {
        val levels = client.interceptors.filterIsInstance<HttpLoggingInterceptor>().map { it.level }
        assertFalse("BODY logging would write API keys and prompts to logcat",
            levels.contains(HttpLoggingInterceptor.Level.BODY))
        assertFalse(levels.contains(HttpLoggingInterceptor.Level.HEADERS))
    }
}
