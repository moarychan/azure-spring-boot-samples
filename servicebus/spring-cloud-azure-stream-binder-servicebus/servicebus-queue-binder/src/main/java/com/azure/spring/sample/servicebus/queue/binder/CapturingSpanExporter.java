package com.azure.spring.sample.servicebus.queue.binder;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A minimal in-memory {@link SpanExporter} that keeps every exported span so the reproducer can
 * assert which spans the OpenTelemetry SDK actually produced.
 *
 * <p>This is how we prove issue #49742: NO {@code ServiceBus.*} spans are ever exported even though
 * the tracing customizer ran, while manually created spans ARE captured (so the SDK and this
 * exporter themselves work fine).
 */
public class CapturingSpanExporter implements SpanExporter {

    private final Queue<SpanData> captured = new ConcurrentLinkedQueue<>();

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        captured.addAll(spans);
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        captured.clear();
        return CompletableResultCode.ofSuccess();
    }

    /**
     * @return a snapshot of all spans captured so far.
     */
    public List<SpanData> getCapturedSpans() {
        return new ArrayList<>(captured);
    }

    /**
     * Clears the captured spans.
     */
    public void clear() {
        captured.clear();
    }
}
