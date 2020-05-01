package no.nav.eessi.pensjon.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MetricsHelper(val registry: MeterRegistry, @Autowired(required = false) val configuration: Configuration = Configuration()) {

    fun init(method: String,
             meterName: String = configuration.measureMeterName,
             success: String = configuration.successTypeTagValue,
             failure: String = configuration.failureTypeTagValue) = Metric(method, meterName, success, failure)

    inner class Metric(
            private val method: String,
            private val meterName: String = configuration.measureMeterName,
            private val success: String = configuration.successTypeTagValue,
            private val failure: String = configuration.failureTypeTagValue) {

        /**
         * Alle counters må legges inn i init'es lik at counteren med konkrete tagger blir initiert med 0.
         * Dette er nødvendig for at grafana alarmer skal fungere i alle tilfeller.
         */
        init {
            Counter.builder(meterName)
                    .tag(configuration.typeTag, success)
                    .tag(configuration.methodTag, method)
                    .register(registry)

            Counter.builder(meterName)
                    .tag(configuration.typeTag, failure)
                    .tag(configuration.methodTag, method)
                    .register(registry)
        }

        fun <R> measure(block: () -> R): R {

            var typeTag = success

            try {
                return Timer.builder("$meterName.${configuration.measureTimerSuffix}")
                        .tag(configuration.methodTag, method)
                        .register(registry)
                        .recordCallable {
                            block.invoke()
                        }
            } catch (throwable: Throwable) {
                typeTag = failure
                throw throwable
            } finally {
                try {
                    Counter.builder(meterName)
                            .tag(configuration.methodTag, method)
                            .tag(configuration.typeTag, typeTag)
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

            val successTypeTagValue: String = "successful",
            val failureTypeTagValue: String = "failed"
    )
}
