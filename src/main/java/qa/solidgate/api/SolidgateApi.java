package qa.solidgate.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qa.solidgate.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class SolidgateApi {

    private static final String INIT_PATH = "/api/v1/init";
    private static final String STATUS_PATH = "/api/v1/status";
    private static final Logger log = LoggerFactory.getLogger(SolidgateApi.class);

    private final HttpClient http;
    private final ObjectMapper json;
    private final Signer signer;
    private final String merchantId;
    private final String apiBaseUrl;
    private final String paymentPageBaseUrl;

    public SolidgateApi(Config cfg) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.json = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.signer = new Signer(cfg.publicKey(), cfg.secretKey());
        this.merchantId = cfg.publicKey();
        this.apiBaseUrl = stripTrailingSlash(cfg.apiBaseUrl());
        this.paymentPageBaseUrl = stripTrailingSlash(cfg.paymentPageBaseUrl());
    }

    public PaymentPage initPaymentPage(OrderRequest request) {
        return post(paymentPageBaseUrl + INIT_PATH, request, PaymentPage.class);
    }

    public OrderStatus orderStatus(String orderId) {
        return post(apiBaseUrl + STATUS_PATH, Map.of("order_id", orderId), OrderStatus.class);
    }

    private <T> T post(String url, Object body, Class<T> type) {
        String payload = serialize(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Merchant", merchantId)
                .header("Signature", signer.sign(payload))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException("Network error calling " + url, e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new ApiException("POST " + url + " returned " + response.statusCode()
                    + ", body: " + response.body());
        }

        rejectIfErrorPayload(url, response.body());
        return deserialize(response.body(), type);
    }

    private void rejectIfErrorPayload(String url, String body) {
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (IOException e) {
            log.debug("Cannot parse response from {} as JSON, deferring to deserializer", url, e);
            return;
        }
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new ApiException("POST " + url + " responded with error: " + error);
        }
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (IOException e) {
            throw new ApiException("Cannot serialize request body", e);
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        try {
            return json.readValue(body, type);
        } catch (IOException e) {
            throw new ApiException("Cannot parse response: " + body, e);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
