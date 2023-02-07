package no.nav.eessi.pensjon.metrics

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.NamingConvention
import io.micrometer.core.instrument.search.Search
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.io.IOException
import java.net.URI
import java.util.stream.Collectors

@ExtendWith(MockKExtension::class)
class RequestCountInterceptorTest {

    private val someUri = URI("/abc")
    private val someInvolvedUri = URI("https://server.example.com:666/path/morepath/7897/path/578?field=74289&field2=secret")
    private val someSimplifedUri = "server:666/path/morepath/{}/path/{}?field={}&field2={}"
    private val httpGet = HttpMethod.GET
    private val httpPost = HttpMethod.POST

    private val mockRequest = MockClientHttpRequest()

    private val mockExecution = mockk<ClientHttpRequestExecution>()
    private val meterRegistry = SimpleMeterRegistry()
    private val requestCountInterceptor = RequestCountInterceptor(meterRegistry)

    private val aBody = "BODY".toByteArray()
    private val responseBody = "RESPONSE BODY".toByteArray()

    @BeforeEach
    fun setup() {
    }

    @AfterEach
    fun `should always call downstream`() {
        verify {mockExecution.execute(any(), any()) }
    }

    @Test
    fun `should call downstream in normal case`() {
        every { mockExecution.execute(any(), any())} returns MockClientHttpResponse(responseBody, HttpStatus.OK)
        requestCountInterceptor.intercept(mockRequest, aBody, mockExecution)
        verify { mockExecution.execute(any(), any()) }
    }

    @Test
    fun `should count successful call`() {
        mockRequest.method = httpGet
        mockRequest.uri = someUri

        every { mockExecution.execute(any(), any()) } returns MockClientHttpResponse(responseBody, HttpStatus.OK)

        requestCountInterceptor.intercept(mockRequest, aBody, mockExecution)

        assertCount(1, httpGet.name(), someUri.toString(), RequestCountInterceptor.SUCCESS_VALUE, 200, "none")
    }

    @Test
    fun `should simplify uris to remove ids and detail info`() {
        mockRequest.method = httpGet
        mockRequest.uri = someInvolvedUri

        every { mockExecution.execute(any(), any()) } returns MockClientHttpResponse(responseBody, HttpStatus.OK)

        requestCountInterceptor.intercept(mockRequest, aBody, mockExecution)

        assertCount(1, httpGet.name(), someSimplifedUri, RequestCountInterceptor.SUCCESS_VALUE, 200, "none")
    }

    @Test
    fun `should count failed call`() {
        mockRequest.method = httpPost
        mockRequest.uri = someUri

        every { mockExecution.execute(any(), any()) } returns MockClientHttpResponse(responseBody, HttpStatus.BAD_REQUEST)

        requestCountInterceptor.intercept(mockRequest, aBody, mockExecution)

        assertCount(1, httpPost.name(), someUri.toString(), RequestCountInterceptor.FAILURE_VALUE, 400, RequestCountInterceptor.UNKNOWN_EXCEPTION_TAG_VALUE)
    }

    @Test
    fun `should count IOException call`() {
        mockRequest.method = httpPost
        mockRequest.uri = someUri

        every { mockExecution.execute(any(), any()) } throws IOException()

        assertThrows<IOException> {
            requestCountInterceptor.intercept(mockRequest, aBody, mockExecution)
        }

        assertCount(1, httpPost.name(), someUri.toString(), RequestCountInterceptor.FAILURE_VALUE, RequestCountInterceptor.UNKNOWN_STATUS_TAG_VALUE, "IOException")
    }

    private fun assertCount(expectedCount: Int, httpMethod: String, uri: String, type: String, status: Int, exception: String) {
        val counterToCheck = meterRegistry.counter(
                RequestCountInterceptor.COUNTER_METER_NAME,
                RequestCountInterceptor.HTTP_METHOD_TAG, httpMethod,
                RequestCountInterceptor.URI_TAG, uri,
                RequestCountInterceptor.TYPE_TAG, type,
                RequestCountInterceptor.STATUS_TAG, status.toString(),
                RequestCountInterceptor.EXCEPTION_TAG, exception)

        assertEquals(
                expectedCount.toDouble(),
                counterToCheck.count(),
                0.1,
                "\nexpected: ${meterName(counterToCheck.id) }: $expectedCount, but found:\n${counterList(meterRegistry)}")
    }

    private fun counterList(meterRegistry: MeterRegistry) =
            Search.`in`(meterRegistry).counters().map { "${meterName(it.id) } : ${it.count()}" }.joinToString("\n")

    private fun meterName(id: Meter.Id) =
            id.getConventionName(NamingConvention.snakeCase) + id.getConventionTags(NamingConvention.snakeCase).stream().map { tag -> "{${tag.key}=\\\"${tag.value}\\\"}" }.collect(Collectors.joining())

}
