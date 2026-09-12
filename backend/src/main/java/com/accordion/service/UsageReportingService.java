package com.accordion.service;

import com.accordion.trace.TraceClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * Reports that this Accordion backend is in use to the
 * <a href="https://github.com/Stephenson-Software/trace">trace</a> usage service.
 *
 * <p>Two events are sent, and nothing else: {@code startup} once the application is ready to
 * serve requests, and {@code channel-created} each time a channel is created through the API.
 * Each carries only the program name ({@code accordion}) and, on {@code startup}, the backend
 * version. Nothing about users, messages, channel names or the host is ever included.
 *
 * <p>Every call returns immediately and never throws: the HTTP request runs on the vendored
 * client's own daemon thread, at most 256 reports wait to be sent before new ones are dropped,
 * and a trace server that is down, slow or rejecting the key costs nothing beyond a dropped
 * report. Reporting is on by default and turned off with {@code usage-reporting.enabled=false}
 * ({@code USAGE_REPORTING_ENABLED=false}); one INFO line at every start says which it is.
 */
@Service
public class UsageReportingService {

    private static final Logger log = LoggerFactory.getLogger(UsageReportingService.class);

    /** The {@code application} the program key was issued for. */
    static final String APPLICATION = "accordion";
    static final String STARTUP_EVENT = "startup";
    static final String CHANNEL_CREATED_EVENT = "channel-created";

    private final TraceClient client;
    private final String version;

    public UsageReportingService(
            @Value("${usage-reporting.enabled:true}") boolean enabled,
            @Value("${usage-reporting.endpoint:https://trace.danielstephenson.dev}") String endpoint,
            @Value("${usage-reporting.key:}") String key,
            @Value("${accordion.version:unknown}") String version) {
        this.version = version == null || version.isBlank() ? "unknown" : version;
        this.client = buildClient(enabled, endpoint, key);
        if (client.isEnabled()) {
            log.info("Usage reporting is on: accordion sends a startup event (program name and version"
                    + " only) and a channel-created event (program name only) to {}. Turn it off with"
                    + " USAGE_REPORTING_ENABLED=false (usage-reporting.enabled).", endpoint);
        } else {
            log.info("Usage reporting is off.");
        }
    }

    private static TraceClient buildClient(boolean enabled, String endpoint, String key) {
        if (!enabled || endpoint == null || endpoint.isBlank()) {
            return TraceClient.disabled();
        }
        return TraceClient.builder(endpoint, APPLICATION)
                .key(key)
                .enabled(true)
                .logger(java.util.logging.Logger.getLogger(UsageReportingService.class.getName()))
                .build();
    }

    /** Whether reports will actually be sent (false when disabled or without a key). */
    public boolean isEnabled() {
        return client.isEnabled();
    }

    /** The tags attached to the startup event: the backend version, nothing else. */
    Map<String, String> startupTags() {
        return Collections.singletonMap("version", version);
    }

    /** Sends the one {@code startup} event once the application is ready to serve requests. */
    @EventListener(ApplicationReadyEvent.class)
    public void reportStartup() {
        client.report(STARTUP_EVENT, null, startupTags());
    }

    /** Sends a {@code channel-created} event. The channel itself is not described. */
    public void reportChannelCreated() {
        client.report(CHANNEL_CREATED_EVENT);
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}
