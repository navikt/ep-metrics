package no.nav.eessi.pensjon.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException

@Component
class MetricsHelper(val registry: MeterRegistry, @Autowired(required = false) val configuration: Configuration = Configuration()) {

    fun init(method: String,
             meterName: String = configuration.measureMeterName,
             success: String = configuration.successTypeTagValue,
             failure: String = configuration.failureTypeTagValue,
             toggleOn: String = configuration.toggleOnTagValue,
             toggleOff: String = configuration.toggleOffTagValue,
             ignoreHttpCodes: List<HttpStatus> = emptyList(),
             alert: Toggle = Toggle.ON) = Metric(method, meterName, success, failure, toggleOn, toggleOff, ignoreHttpCodes, alert)

    enum class Toggle { ON, OFF;
        fun text(toggleOn: String, toggleOff: String) =
                if (this == ON) toggleOn else toggleOff
    }

    inner class Metric(
            private val method: String,
            private val meterName: String = configuration.measureMeterName,
            private val success: String = configuration.successTypeTagValue,
            private val failure: String = configuration.failureTypeTagValue,
            private val toggleOn: String = configuration.toggleOnTagValue,
            private val toggleOff: String = configuration.toggleOffTagValue,
            private val ignoreHttpCodes: List<HttpStatus>,
            private val alert: Toggle = Toggle.ON) {

        /**
         * Alle counters må legges inn i init'es lik at counteren med konkrete tagger blir initiert med 0.
         * Dette er nødvendig for at grafana alarmer skal fungere i alle tilfeller.
         */
        init {
            Counter.builder(meterName)
                .tag(configuration.typeTag, success)
                .tag(configuration.methodTag, method)
                .tag(configuration.alertTag, toggleOn)
                .register(registry)

            Counter.builder(meterName)
                .tag(configuration.typeTag, success)
                .tag(configuration.methodTag, method)
                .tag(configuration.alertTag, toggleOff)
                .register(registry)

            Counter.builder(meterName)
                .tag(configuration.typeTag, failure)
                .tag(configuration.methodTag, method)
                .tag(configuration.alertTag, toggleOn)
                .register(registry)

            Counter.builder(meterName)
                .tag(configuration.typeTag, failure)
                .tag(configuration.methodTag, method)
                .tag(configuration.alertTag, toggleOff)
                .register(registry)
        }

        fun <R> measure(block: () -> R): R {

            var typeTag = success
            var ignoreErrorCode = false

            try {
                return Timer.builder("$meterName.${configuration.measureTimerSuffix}")
                        .tag(configuration.methodTag, method)
                        .register(registry)
                        .recordCallable {
                            block.invoke()
                        }
            } catch (throwable: Throwable) {
                if(throwable is HttpStatusCodeException && throwable.statusCode in ignoreHttpCodes) ignoreErrorCode = true
                typeTag = failure
                throw throwable
            } finally {
                try {
                    Counter.builder(meterName)
                            .tag(configuration.methodTag, method)
                            .tag(configuration.typeTag, typeTag)
                            .tag(configuration.alertTag, if(ignoreErrorCode) configuration.toggleOffTagValue else alert.text(toggleOn, toggleOff))
                            .register(registry)
                            .increment()
                } catch (e: Exception) {
                    // ignoring on purpose
                }
            }
        }
    }


    data class Configuration(
            val incrementMeterName: String = "event",
            val measureMeterName: String = "method",
            val measureTimerSuffix: String = "timer",

            val eventTag: String = "event",
            val methodTag: String = "method",
            val typeTag: String = "type",
            val alertTag: String = "alert",

            val successTypeTagValue: String = "successful",
            val failureTypeTagValue: String = "failed",

            val toggleOnTagValue: String = "on",
            val toggleOffTagValue: String = "off"
    )
}
