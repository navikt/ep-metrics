package no.nav.eessi.pensjon.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.io.IOError
import java.util.concurrent.TimeUnit

internal class MetricsHelperTest {

    lateinit var registry: MeterRegistry
    lateinit var metricsHelper: MetricsHelper
    lateinit var config: MetricsHelper.Configuration

    @BeforeEach
    fun setup() {
        registry = SimpleMeterRegistry()
        config = MetricsHelper.Configuration()
        metricsHelper = MetricsHelper(registry)
    }

    @Test
    fun measure_counts_success_up_by_one_if_no_exception() {
        val dummy = metricsHelper.init("dummy")
        dummy.measure {
            // the logic you want measured - no exception happening her today
        }

        assertEquals(
                1.0,
                registry.counter(
                        config.measureMeterName,
                        config.methodTag, "dummy",
                        config.alertTag, config.toggleOnTagValue,
                        config.typeTag, config.successTypeTagValue)
                        .count(),
                0.0001)
    }

    @Test
    fun measure_counts_failure_up_by_one_if_exception_thrown() {
        val dummy = metricsHelper.init("dummy")
        try {
            dummy.measure {
                throw RuntimeException("boom!")
            }
        } catch (ex: RuntimeException) {
            // ignoring on purpose
        }

        assertEquals(
                1.0,
                registry.counter(
                        config.measureMeterName,
                        config.methodTag, "dummy",
                        config.alertTag, config.toggleOnTagValue,
                        config.typeTag, config.failureTypeTagValue)
                        .count(),
                0.0001)
    }

    @Test
    fun measure_counts_failure_up_by_one_if_error_thrown() {
        val dummy = metricsHelper.init("dummy")
        try {
            dummy.measure {
                throw IOError(RuntimeException())
            }
        } catch (ex: IOError) {
            // ignoring on purpose
        }

        assertEquals(
                1.0,
                registry.counter(
                        config.measureMeterName,
                        config.methodTag, "dummy",
                        config.alertTag, config.toggleOnTagValue,
                        config.typeTag, config.failureTypeTagValue)
                        .count(),
                0.0001)
    }

    @Test
    fun measure_registers_a_timer_too() {
        val mockClock = MockClock()
        val registry = SimpleMeterRegistry(SimpleConfig.DEFAULT, mockClock)
        val metricsHelper = MetricsHelper(registry)

        val dummy = metricsHelper.init("dummy")
        dummy.measure {
            // the logic you want counted - no exception happening her today
            mockClock.add(100, TimeUnit.MILLISECONDS)
        }

        val timer = registry.timer(
                "${config.measureMeterName}.${config.measureTimerSuffix}",
                config.methodTag, "dummy")
        assertEquals(1, timer.count())
        assertEquals(
                100.0,
                timer.totalTime(TimeUnit.MILLISECONDS),
                10.0)
    }

    @Test
    fun `measure accepts an alert parameter that results in a alert-tag`() {
        val dummy = metricsHelper.init("dummy", alert = MetricsHelper.Toggle.OFF)
        try {
            dummy.measure {
                throw IOError(RuntimeException())
            }
        } catch (ex: IOError) {
            // ignoring on purpose
        }

        // when

        // then
        assertEquals(
                1.0,
                registry.counter(
                        config.measureMeterName,
                        config.methodTag, "dummy",
                        config.alertTag, config.toggleOffTagValue,
                        config.typeTag, config.failureTypeTagValue)
                        .count(),
                0.0001)

    }

    @Test
    fun `default is alert on`() {
        val dummy = metricsHelper.init("dummy")

        try {
            dummy.measure {
                throw IOError(RuntimeException())
            }
        } catch (ex: IOError) {
            // ignoring on purpose
        }

        assertEquals(
                1.0,
                registry.counter(
                        config.measureMeterName,
                        config.methodTag, "dummy",
                        config.alertTag, "on",
                        config.typeTag, config.failureTypeTagValue)
                        .count(),
                0.0001)

    }

    @Test
    fun `given a 404 httpCode and ignore 404 when measuring then alert off`() {
        val dummy = metricsHelper.init(method="dummy",ignoreHttpCodes = listOf(HttpStatus.NOT_FOUND))

        try {
            dummy.measure {
                throw HttpClientErrorException(HttpStatus.NOT_FOUND)
            }
        } catch (ex: HttpClientErrorException) {
            // ignoring on purpose
        }

        assertEquals(
            1.0,
            registry.counter(
                config.measureMeterName,
                config.methodTag, "dummy",
                config.alertTag, "off",
                config.typeTag, config.failureTypeTagValue)
                .count(),
            0.0001)

    }

}
