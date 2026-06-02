package dev.maggi.burplinkcord.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.maggi.burplinkcord.api.controller.DiscordController;
import dev.maggi.burplinkcord.api.controller.HealthController;
import dev.maggi.burplinkcord.api.controller.IssueController;
import dev.maggi.burplinkcord.api.controller.ScanController;
import dev.maggi.burplinkcord.api.request.StartScanRequest;
import dev.maggi.burplinkcord.api.request.StopScanRequest;
import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.domain.service.InMemoryStatusService;
import dev.maggi.burplinkcord.events.EventBus;
import dev.maggi.burplinkcord.events.RuntimeEvent;
import dev.maggi.burplinkcord.exception.AuthenticationException;
import dev.maggi.burplinkcord.exception.AuthorizationException;
import dev.maggi.burplinkcord.exception.BurpLinkCordException;
import dev.maggi.burplinkcord.logging.AuditLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Embedded local API server for BurpLinkCord.
 */
public class ApiServer {

    private final ApplicationConfiguration applicationConfiguration;
    private final ObjectMapper objectMapper;
    private final HealthController healthController;
    private final DiscordController discordController;
    private final ScanController scanController;
    private final IssueController issueController;
    private final InMemoryStatusService statusService;
    private final EventBus eventBus;
    private final AuditLogger auditLogger;
    private HttpServer httpServer;

    /**
     * Creates an API server.
     *
     * @param applicationConfiguration bootstrap configuration
     * @param objectMapper JSON mapper
     * @param healthController health controller
     * @param discordController Discord controller
     * @param scanController scan controller
     * @param issueController issue controller
     * @param statusService status service
     * @param eventBus event bus
     * @param auditLogger audit logger
     */
    public ApiServer(
            ApplicationConfiguration applicationConfiguration,
            ObjectMapper objectMapper,
            HealthController healthController,
            DiscordController discordController,
            ScanController scanController,
            IssueController issueController,
            InMemoryStatusService statusService,
            EventBus eventBus,
            AuditLogger auditLogger
    ) {
        this.applicationConfiguration = Objects.requireNonNull(applicationConfiguration, "applicationConfiguration");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.healthController = Objects.requireNonNull(healthController, "healthController");
        this.discordController = Objects.requireNonNull(discordController, "discordController");
        this.scanController = Objects.requireNonNull(scanController, "scanController");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    /**
     * Starts the API server if it is not already running.
     */
    public synchronized void start() {
        if (httpServer != null) {
            return;
        }

        try {
            httpServer = HttpServer.create(
                    new InetSocketAddress(applicationConfiguration.serverHost(), applicationConfiguration.serverPort()),
                    0
            );
            httpServer.createContext("/health", exchange -> handleJson(exchange, "GET", ignored -> healthController.getHealth()));
            httpServer.createContext("/scans", exchange -> handleJson(exchange, "GET", ignored -> scanController.getScans(header(exchange))));
            httpServer.createContext("/scans/start", exchange -> handleJson(exchange, "POST",
                    body -> scanController.startScan(header(exchange), readBody(body, StartScanRequest.class))));
            httpServer.createContext("/scans/stop", exchange -> handleJson(exchange, "POST",
                    body -> scanController.stopScan(header(exchange), readBody(body, StopScanRequest.class))));
            httpServer.createContext("/scans/pause", exchange -> handleJson(exchange, "POST",
                    body -> scanController.pauseScan(header(exchange), readBody(body, StopScanRequest.class).scanId())));
            httpServer.createContext("/scans/resume", exchange -> handleJson(exchange, "POST",
                    body -> scanController.resumeScan(header(exchange), readBody(body, StopScanRequest.class).scanId())));
            httpServer.createContext("/scans/delete", exchange -> handleJson(exchange, "POST",
                    body -> scanController.deleteScan(header(exchange), readBody(body, StopScanRequest.class).scanId())));
            httpServer.createContext("/discord", exchange -> handleJson(exchange, "GET", ignored -> discordController.getStatus(header(exchange))));
            httpServer.createContext("/discord/start", exchange -> handleJson(exchange, "POST", ignored -> discordController.start(header(exchange))));
            httpServer.createContext("/discord/stop", exchange -> handleJson(exchange, "POST", ignored -> discordController.stop(header(exchange))));
            httpServer.createContext("/discord/panel", exchange -> handleJson(exchange, "POST", ignored -> discordController.publishControlPanel(header(exchange))));
            httpServer.createContext("/issues", exchange -> handleJson(exchange, "GET", ignored -> issueController.getIssues(header(exchange))));
            httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            httpServer.start();
            statusService.markApiRunning(true);
            statusService.setMessage("Local API running");
            statusService.recordActivity("Local API started on " + applicationConfiguration.serverHost() + ":" + applicationConfiguration.serverPort());
            eventBus.publish("runtime.notification", new RuntimeEvent(
                    "Local API Started",
                    "BurpLinkCord local API is listening on %s:%d.".formatted(
                            applicationConfiguration.serverHost(),
                            applicationConfiguration.serverPort()
                    ),
                    "INFO"
            ));
            auditLogger.log("api.start host=" + applicationConfiguration.serverHost() + " port=" + applicationConfiguration.serverPort());
        } catch (IOException exception) {
            throw new BurpLinkCordException("Unable to start local API server.", exception);
        }
    }

    /**
     * Stops the API server if it is running.
     */
    public synchronized void stop() {
        if (httpServer == null) {
            return;
        }

        httpServer.stop(0);
        httpServer = null;
        statusService.markApiRunning(false);
        statusService.setMessage("Local API stopped");
        statusService.recordActivity("Local API stopped");
        eventBus.publish("runtime.notification", new RuntimeEvent(
                "Local API Stopped",
                "BurpLinkCord local API has stopped.",
                "INFO"
        ));
        auditLogger.log("api.stop");
    }

    /**
     * Returns whether the API server is active.
     *
     * @return true when running
     */
    public synchronized boolean isRunning() {
        return httpServer != null;
    }

    private void handleJson(HttpExchange exchange, String method, JsonResponder responder) throws IOException {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            writeResponse(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try (InputStream body = exchange.getRequestBody()) {
            Object response = responder.respond(body);
            writeResponse(exchange, 200, response);
        } catch (AuthenticationException exception) {
            writeResponse(exchange, 401, Map.of("error", exception.getMessage()));
        } catch (AuthorizationException exception) {
            writeResponse(exchange, 403, Map.of("error", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            writeResponse(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (NoSuchElementException exception) {
            writeResponse(exchange, 404, Map.of("error", "The requested resource was not found."));
        } catch (Exception exception) {
            writeResponse(exchange, 500, Map.of("error", exception.getMessage()));
        }
    }

    private <T> T readBody(InputStream body, Class<T> bodyType) {
        try {
            return objectMapper.readValue(body, bodyType);
        } catch (IOException exception) {
            throw new BurpLinkCordException("Unable to parse request body.", exception);
        }
    }

    private void writeResponse(HttpExchange exchange, int statusCode, Object response) throws IOException {
        byte[] bytes = serialize(response);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private byte[] serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            throw new BurpLinkCordException("Unable to serialize response.", exception);
        }
    }

    private String header(HttpExchange exchange) {
        return exchange.getRequestHeaders().getFirst("Authorization");
    }

    @FunctionalInterface
    private interface JsonResponder {
        Object respond(InputStream body) throws IOException;
    }
}
