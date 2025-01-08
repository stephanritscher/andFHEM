/*
 * AndFHEM - Open Source Android application to control a FHEM home automation
 * server.
 *
 * Copyright (c) 2011, Matthias Klass or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU GENERAL PUBLIC LICENSE, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU GENERAL PUBLIC LICENSE
 * for more details.
 *
 * You should have received a copy of the GNU GENERAL PUBLIC LICENSE
 * along with this distribution; if not, write to:
 *   Free Software Foundation, Inc.
 *   51 Franklin Street, Fifth Floor
 *   Boston, MA  02110-1301  USA
 */

package li.klass.fhem.connection.backend

import android.content.Context
import android.net.TrafficStats
import com.google.android.gms.security.ProviderInstaller
import com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.HttpResponseException
import li.klass.fhem.connection.backend.RequestResultError.*
import li.klass.fhem.connection.backend.ssl.MemorizingTrustManagerContextInitializer
import li.klass.fhem.error.ErrorHolder
import li.klass.fhem.util.ApplicationProperties
import org.slf4j.LoggerFactory
import java.io.*
import java.net.SocketTimeoutException
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException

class FHEMWEBConnection(fhemServerSpec: FHEMServerSpec, applicationProperties: ApplicationProperties) : FHEMConnection(fhemServerSpec, applicationProperties) {

    val basicAuthHeaders: HttpHeaders
        get() {
            val password = server.password ?: ""
            val username = server.username ?: ""
            return HttpHeaders().setBasicAuthentication(username, password)
        }

    val password: String
        get() = server.password ?: ""

    override fun executeCommand(command: String, context: Context): RequestResult<String> {
        LOG.info("executeTask command $command")
        val urlSuffix = generateCommandUrlSuffix(command)
        val result = request(context, urlSuffix)
        if (result is RequestResult.Success && (result.success.contains("<title>") || result.success.contains("<div id="))) {
            LOG.error("found strange content: ${result.success}")
            ErrorHolder.setError("found strange content in URL $urlSuffix: \r\n\r\n${result.success}")
            return RequestResult.Error(INVALID_CONTENT)
        }
        return result
    }

    fun request(context: Context, urlSuffix: String): RequestResult<String> {
        try {
            ProviderInstaller.installIfNeeded(context)
        } catch (e: Exception) {
            LOG.error("cannot install play providers", e)
        }

        return try {
            val response = executeRequest(urlSuffix, context)
            response.map { success ->
                InputStreamReader(success, Charsets.UTF_8).use {
                    it.readText()
                }
            }
        } catch (e: Exception) {
            LOG.error("cannot handle result", e)
            RequestResult.Error(INTERNAL_ERROR)
        }
    }

    private fun generateCommandUrlSuffix(command: String): String {
        try {
            return "?XHR=1&cmd=" + URLEncoder.encode(command, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            LOG.error("unsupported encoding", e)
            throw RuntimeException(e)
        }
    }

    fun executeRequest(urlSuffix: String?, context: Context): RequestResult<InputStream> =
            executeRequest(server.url!!, urlSuffix, false, context)

    private fun executeRequest(serverUrl: String, urlSuffix: String?, isRetry: Boolean, context: Context): RequestResult<InputStream> {
        TrafficStats.setThreadStatsTag(1)
        try {
            initSslContext(context)

            val url = serverUrl + if (urlSuffix?.contains("cmd=") == true) {
                val csrfToken = findCsrfToken(serverUrl) ?: ""
                "$urlSuffix&fwcsrf=$csrfToken"
            } else urlSuffix
            LOG.info("accessing URL {}", url)

            val response = doGet(url)

            LOG.debug("response status code is " + response.statusCode)


            return RequestResult.Success(BufferedInputStream(response.content) as InputStream)
        } catch (e: HttpResponseException) {
            val errorResult = handleHttpStatusCode(e.statusCode)
            val msg = "found error " + (errorResult?.error?.declaringJavaClass?.simpleName
                    ?: "unknown") + " for " +
                    "status code " + e.statusCode
            LOG.debug(msg)
            ErrorHolder.setError(null, msg)
            return errorResult ?: RequestResult.Error(INTERNAL_ERROR)

        } catch (e: Exception) {
            LOG.info("error while loading data", e)
            return handleError(urlSuffix ?: "", isRetry, serverUrl, e, context)
        }

    }

    private fun findCsrfToken(serverUrl: String): String? {
        if (server.csrfToken != null) {
            LOG.info("using predefined csrf token from server spec")
            return server.csrfToken
        }
        return try {
            val response = doGet("$serverUrl?room=notExistingJustToLoadCsrfToken")
            val value = response.headers.getFirstHeaderStringValue("X-FHEM-csrfToken")
            response.content.close()
            value?.let { URLEncoder.encode(it, "UTF-8") }
        } catch (e: SocketTimeoutException) {
            LOG.info("socket timed out", e)
            throw e
        } catch (e: SSLHandshakeException) {
            LOG.info("ssl handshake failed", e)
            throw e
        } catch (e: IOException) {
            LOG.info("cannot load csrf token", e)
            null
        }
    }

    private fun doGet(url: String): HttpResponse {
        return newCompatibleTransport()
                .createRequestFactory().buildGetRequest(GenericUrl(url))
                .setConnectTimeout(SOCKET_TIMEOUT)
                .setReadTimeout(SOCKET_TIMEOUT)
                .setLoggingEnabled(false)
                .setHeaders(basicAuthHeaders)
                .execute()
    }

    private fun handleError(urlSuffix: String, isRetry: Boolean, url: String, e: Exception, context: Context): RequestResult<InputStream> {
        setErrorInErrorHolderFor(e, url, urlSuffix)
        return handleRetryIfRequired(isRetry, RequestResult.Error(HOST_CONNECTION_ERROR), urlSuffix, context)
    }

    private fun handleRetryIfRequired(isCurrentRequestRetry: Boolean, previousResult: RequestResult<InputStream>,
                                      urlSuffix: String, context: Context): RequestResult<InputStream> {
        return if (!server.canRetry() || isCurrentRequestRetry || server.alternateUrl == null) {
            previousResult
        } else retryRequest(urlSuffix, context)
    }

    private fun retryRequest(urlSuffix: String, context: Context): RequestResult<InputStream> {
        LOG.info("retrying request for alternate URL")
        return executeRequest(server.alternateUrl!!, urlSuffix, true, context)
    }

    private fun initSslContext(context: Context) {
        MemorizingTrustManagerContextInitializer().init(context, server)
                ?.let {
                    HttpsURLConnection.setDefaultSSLSocketFactory(it.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier(it.hostnameVerifier)
                }
    }

    companion object {
        const val SOCKET_TIMEOUT = 60000
        private val LOG = LoggerFactory.getLogger(FHEMWEBConnection::class.java)
        private val STATUS_CODE_MAP: Map<Int, RequestResultError> = mapOf(
                400 to BAD_REQUEST,
                401 to AUTHENTICATION_ERROR,
                403 to AUTHENTICATION_ERROR,
                404 to NOT_FOUND,
                500 to INTERNAL_SERVER_ERROR
        )

        init {
            HttpsURLConnection.setDefaultSSLSocketFactory(NoSSLv3Factory())
        }

        private fun handleHttpStatusCode(statusCode: Int): RequestResult.Error<InputStream>? {
            val error = STATUS_CODE_MAP[statusCode] ?: return null

            LOG.info("handleHttpStatusCode() : encountered http status code {}", statusCode)
            return RequestResult.Error(error)
        }
    }
}
