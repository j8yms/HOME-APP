package com.example.householdapp.core.network

import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

/**
 * Regression tests for the crash loop:
 *
 * JsonResponseInterceptor used to throw IOException on an HTML response
 * WITHOUT closing it. OkHttp 3.14's Transmitter then still had an open
 * exchange, so the retrying ResilientAppsScriptInterceptor's next
 * chain.proceed() threw a bare IllegalStateException on the OkHttp Dispatcher
 * thread - an uncaught exception that killed the whole process.
 */
class InterceptorRetryTest {

    @get:Rule
    val server = MockWebServer()

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ResilientAppsScriptInterceptor())
        .addInterceptor(JsonResponseInterceptor())
        .build()

    private fun htmlResponse(): MockResponse = MockResponse()
        .setResponseCode(404)
        .setHeader("Content-Type", "text/html; charset=utf-8")
        .setBody("<!DOCTYPE html><html><body>Service error</body></html>")

    private fun jsonResponse(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json; charset=utf-8")
        .setBody("""{"success":true,"data":{"status":"ok"}}""")

    private fun execute() {
        val request = Request.Builder()
            .url(server.url("/exec?route=health"))
            .get()
            .build()
        client().newCall(request).execute().use { response ->
            assertEquals(200, response.code())
            assertTrue(response.body()!!.string().contains("\"success\":true"))
        }
    }

    @Test
    fun htmlErrorThenSuccess_isRetriedWithoutCrashing() {
        server.enqueue(htmlResponse())
        server.enqueue(jsonResponse())

        execute()

        assertEquals(2, server.requestCount)
    }

    @Test
    fun persistentHtmlErrors_surfaceAsIOException_notIllegalState() {
        repeat(5) { server.enqueue(htmlResponse()) }

        try {
            execute()
            fail("expected IOException")
        } catch (expected: IOException) {
            assertTrue(
                "message should identify the HTML failure, was: ${expected.message}",
                (expected.message ?: "").contains("HTML")
            )
        } catch (illegal: IllegalStateException) {
            fail(
                "IllegalStateException escaped the interceptor - this kills the " +
                    "OkHttp Dispatcher thread and crashes the app: $illegal"
            )
        }
    }

    @Test
    fun networkFailureBetweenAttempts_doesNotCrashTheDispatcherThread() {
        // Enough queued resets for OkHttp's internal recovery plus this
        // interceptor's attempts - every attempt fails at the transport level.
        repeat(10) {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        }
        try {
            execute()
            fail("expected IOException")
        } catch (expected: IOException) {
            // pass: transport failure reported as IOException
        } catch (illegal: IllegalStateException) {
            fail("unchecked IllegalStateException escaped: $illegal")
        }
    }
}
