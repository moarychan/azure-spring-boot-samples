package com.azure.spring.sample.servicebus.queue.binder;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds an OpenTelemetry SDK and exposes it only as a Spring bean.
 *
 * <p>{@code buildAndRegisterGlobal()} is intentionally NOT used — this mirrors the issue's setup
 * where the SDK is a bean and {@code GlobalOpenTelemetry} is never registered (as happens with the
 * OpenTelemetry Spring Boot starter). Because the global is absent, the Azure SDK can only obtain
 * the tracer through {@code ClientOptions.setTracingOptions(...)} — which is exactly what issue
 * #49742 shows gets discarded for Service Bus processors.
 *
 * <p>Spans are exported to an in-memory {@link CapturingSpanExporter} so the reproducer can verify
 * exactly which spans are produced.
 */
@Configuration(proxyBeanMethods = false)
public class OpenTelemetryConfig {

    @Bean
    CapturingSpanExporter capturingSpanExporter() {
        return new CapturingSpanExporter();
    }

    @Bean(destroyMethod = "close")
    OpenTelemetrySdk openTelemetry(CapturingSpanExporter capturingSpanExporter) {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.getDefault().merge(Resource.create(
                        Attributes.of(AttributeKey.stringKey("service.name"), "sca-reproducer"))))
                .addSpanProcessor(SimpleSpanProcessor.create(capturingSpanExporter))
                .build();
        // build() — NOT buildAndRegisterGlobal(): the SDK travels only via the Spring bean / customizer.
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }
}
