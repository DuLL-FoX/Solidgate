package qa.solidgate.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatus(Order order, Map<String, Transaction> transactions) {

    private static final Set<String> APPROVED_ORDER_STATUSES = Set.of("settle_ok");

    private static final Set<String> SUCCESSFUL_TX_STATUSES = Set.of(
            "success", "settle_ok");

    public boolean isApproved() {
        return order != null && order.status != null
                && APPROVED_ORDER_STATUSES.contains(order.status.toLowerCase());
    }

    public boolean hasSuccessfulTransaction() {
        if (transactions == null) return false;
        return transactions.values().stream()
                .filter(t -> t != null && t.status != null)
                .anyMatch(t -> SUCCESSFUL_TX_STATUSES.contains(t.status.toLowerCase()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(
            @JsonProperty("order_id") String orderId,
            long amount,
            String currency,
            String status,
            @JsonProperty("processing_amount") Long processingAmount,
            @JsonProperty("processing_currency") String processingCurrency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
            String status,
            long amount,
            String currency,
            @JsonProperty("operation") String operation) {
    }
}
