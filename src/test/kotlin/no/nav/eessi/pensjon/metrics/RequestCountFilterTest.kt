package no.nav.eessi.pensjon.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.net.URI
import javax.servlet.FilterChain
import javax.servlet.ServletException


class RequestCountFilterTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val filter = RequestCountFilter(meterRegistry, true)

    private val someUri = "/abc"
    private val someInvolvedUri = "https://server.example.com:666/path/morepath/7897/path/578?field=74289&field2=secret"
    private val someSimplifedUri = "server:666/path/morepath/{}/path/{}?field={}&field2={}"
    private val httpGet = "GET"
    private val httpPost = "POST"
    private val clientError = 400
    private val serverError = 500


    @Test
    fun `should call next in filter chain`() {
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertNotNull(chain.request)
    }

    @Test
    fun `should not count successful calls`() {
        val filter2 = RequestCountFilter(meterRegistry, false)

        val request = MockHttpServletRequest(httpGet, someUri)
        filter2.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertCount(0, httpGet, someUri, SUCCESS_VALUE, 200, NO_EXCEPTION_TAG_VALUE)
    }


    @Test
    fun `should count successful calls`() {
        val request = MockHttpServletRequest(httpGet, someUri)
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertCount(1, httpGet, someUri, SUCCESS_VALUE, 200, NO_EXCEPTION_TAG_VALUE)
    }

    @Test
    fun `should count simplify involved uris`() {
        val request = MockHttpServletRequest(httpGet, someInvolvedUri)
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertCount(1, httpGet, someSimplifedUri, SUCCESS_VALUE, 200, NO_EXCEPTION_TAG_VALUE)
    }

    @Test
    fun `should count client errors as failures`() {
        val request = MockHttpServletRequest(httpGet, someUri)
        val response = MockHttpServletResponse()
        
        response.status = clientError
        filter.doFilter(request, response, MockFilterChain())

        assertCount(1, httpGet, someUri, FAILURE_VALUE, clientError, UNKNOWN_EXCEPTION_TAG_VALUE)
    }


    @Test
    fun `should count server errors as failures`() {
        val request = MockHttpServletRequest(httpPost, someUri)
        val response = MockHttpServletResponse()

        response.status = serverError
        filter.doFilter(request, response, MockFilterChain())

        assertCount(1, httpPost, someUri, FAILURE_VALUE, serverError, UNKNOWN_EXCEPTION_TAG_VALUE)
    }

    @Test
    fun `should propagate exception, but still count it`() {
        val request = MockHttpServletRequest(httpGet, someUri)
        val response = MockHttpServletResponse()

        response.status = serverError
        val mockFilterChain = mockk<FilterChain>()

        every { mockFilterChain.doFilter(any(), any()) } throws ServletException()

        assertThrows<ServletException> {
            filter.doFilter(request, response, mockFilterChain)
        }

        assertCount(1, httpGet, someUri, FAILURE_VALUE, serverError, "ServletException")
    }

    private fun assertCount(count: Int, httpMethod: String, uri: String, type: String, status: Int, exception: String) {
        assertEquals(count.toDouble(),
                meterRegistry.counter(
                        COUNTER_METER_NAME,
                        HTTP_METHOD_TAG, httpMethod,
                        URI_TAG, uri,
                        TYPE_TAG, type,
                        STATUS_TAG, status.toString(),
                        EXCEPTION_TAG, exception
                ).count(), 0.1)
    }

}
