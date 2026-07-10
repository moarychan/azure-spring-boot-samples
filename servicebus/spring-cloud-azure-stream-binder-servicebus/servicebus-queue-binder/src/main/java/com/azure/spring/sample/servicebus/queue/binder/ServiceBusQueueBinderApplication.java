// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.spring.sample.servicebus.queue.binder;

import com.azure.core.tracing.opentelemetry.OpenTelemetryTracingOptions;
import com.azure.core.util.ClientOptions;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.spring.cloud.core.customizer.AzureServiceClientBuilderCustomizer;
import com.azure.spring.messaging.ConsumerIdentifier;
import com.azure.spring.messaging.PropertiesSupplier;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.azure.spring.messaging.servicebus.core.properties.ProcessorProperties;
import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

@SpringBootApplication
public class ServiceBusQueueBinderApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBusQueueBinderApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ServiceBusQueueBinderApplication.class, args);
    }

    @Bean
    public Consumer<Message<String>> consume() {
        return message -> {
            Checkpointer checkpointer = (Checkpointer) message.getHeaders().get(CHECKPOINTER);
            LOGGER.info("New message received: '{}'", message.getPayload());
            checkpointer.success()
                    .doOnSuccess(s -> LOGGER.info("Message '{}' successfully checkpointed", message.getPayload()))
                    .doOnError(e -> LOGGER.error("Error found", e))
                    .block();
        };
    }

    @Bean
    AzureServiceClientBuilderCustomizer<ServiceBusClientBuilder> serviceBusTracingCustomizer(
            OpenTelemetry openTelemetry) {
        return builder -> {
            LOGGER.info("[ServiceBusTracingConfig] customizer applying OpenTelemetry TracingOptions to the "
                    + "root ServiceBusClientBuilder (issue #49742 — expected to be overwritten before the "
                    + "@ServiceBusListener processor client is built)");
            builder.clientOptions(new ClientOptions().setTracingOptions(
                    new OpenTelemetryTracingOptions().setOpenTelemetry(openTelemetry)));
        };
    }

    @Bean
    PropertiesSupplier<ConsumerIdentifier, ProcessorProperties> processorPropertiesSupplier() {
        return id -> {
            ProcessorProperties processorProperties = new ProcessorProperties();
            processorProperties.setEntityName("reproducer-queue");
            if (id.getDestination().equals("reproducer-queue")) {
                processorProperties.setInheritConfiguration(false);
            }
            return processorProperties;
        };
    }
}
