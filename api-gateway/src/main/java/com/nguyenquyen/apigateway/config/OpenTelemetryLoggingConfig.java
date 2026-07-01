package com.nguyenquyen.apigateway.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenTelemetryLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryLoggingConfig.class);

    @Bean
    @Primary
    public LogRecordExporter otlpHttpLogRecordExporter(@Value("${management.otlp.logs.export.url:http://localhost:4318/v1/logs}") String endpoint) {
        return OtlpHttpLogRecordExporter.builder()
                .setEndpoint(endpoint)
                .build();
    }

    @Bean
    @Primary
    public SdkLoggerProvider sdkLoggerProvider(LogRecordExporter logRecordExporter, ObjectProvider<Resource> resourceProvider) {
        Resource resource = resourceProvider.getIfAvailable(Resource::getDefault);
        return SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(logRecordExporter).build())
                .build();
    }

    @Bean
    public CommandLineRunner openTelemetryLogbackInstaller(ObjectProvider<OpenTelemetry> openTelemetryProvider,
                                                           ObjectProvider<SdkLoggerProvider> sdkLoggerProviderProvider) {
        return args -> {
            try {
                OpenTelemetry baseOtel = openTelemetryProvider.getIfAvailable();
                SdkLoggerProvider loggerProvider = sdkLoggerProviderProvider.getIfAvailable();
                if (baseOtel != null && loggerProvider != null) {
                    OpenTelemetry wrappedOtel = new OpenTelemetry() {
                        @Override
                        public TracerProvider getTracerProvider() { return baseOtel.getTracerProvider(); }
                        @Override
                        public MeterProvider getMeterProvider() { return baseOtel.getMeterProvider(); }
                        @Override
                        public ContextPropagators getPropagators() { return baseOtel.getPropagators(); }
                        @Override
                        public LoggerProvider getLogsBridge() { return loggerProvider; }
                    };
                    OpenTelemetryAppender.install(wrappedOtel);
                    log.info("Successfully installed wrapped OpenTelemetry with explicit SdkLoggerProvider to Logback!");
                    loggerProvider.forceFlush();
                } else if (baseOtel != null) {
                    OpenTelemetryAppender.install(baseOtel);
                    log.info("Installed default OpenTelemetry to Logback!");
                }
            } catch (Exception e) {
                log.warn("Notice during OpenTelemetry Logback installation: {}", e.getMessage());
            }
        };
    }
}
