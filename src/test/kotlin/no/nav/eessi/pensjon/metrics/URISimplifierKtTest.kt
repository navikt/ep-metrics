package no.nav.eessi.pensjon.metrics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.util.stream.Stream

internal class URISimplifierKtTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("eksemplerSomString")
    fun simplifyUri(from: String, to: String) {
        assertEquals(to, simplifyUri(from))
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("eksemplerSomUri")
    fun testSimplifyUri(from: URI, to: String) {
        assertEquals(to, simplifyUri(from))
    }

    private companion object {
        @JvmStatic
        fun eksemplerSomUri() = Stream.of(
            Arguments.of(URI("/sed"), "/sed"),
            Arguments.of(URI("/v2/sed"), "/v2/sed"),
            Arguments.of(URI("/sed/687"), "/sed/{}"),
            Arguments.of(URI("/sed/7897/something/789788"), "/sed/{}/something/{}"),
            Arguments.of(URI("http:/sed"), "/sed"),
            Arguments.of(URI("http://yo.yo/sed"), "yo:/sed"),
            Arguments.of(URI("/zed#'sdeadbabe"), "/zed"),
            Arguments.of(
                URI("https://eux-rina-api.prod-fss-pub.nais.io/cpi/buc/90029410/sed/0bcf35362a2440a28d17d9b8a33e5ba1"),
                "eux-rina-api:/cpi/buc/{}/sed/{}"),
            Arguments.of(
                URI("https://server.example.com:666/path/morepath/7897/path/578?field=74289&field2=secret"),
                "server:666/path/morepath/{}/path/{}?field={}&field2={}"
            ),
        )

        @JvmStatic
        fun eksemplerSomString() =
            eksemplerSomUri().map { Arguments.of(it.get()[0].toString(), it.get()[1]) }
    }
}