package qa.solidgate.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderRequest(
        Order order,
        @JsonProperty("page_customization") PageCustomization pageCustomization) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Order(
            @JsonProperty("order_id") String orderId,
            long amount,
            String currency,
            @JsonProperty("order_description") String description,
            @JsonProperty("type") String type,
            @JsonProperty("customer_email") String customerEmail,
            @JsonProperty("settle_interval") Integer settleInterval) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PageCustomization(
            @JsonProperty("public_name") String publicName,
            @JsonProperty("order_title") String orderTitle,
            @JsonProperty("order_description") String orderDescription) {
    }

    public static OrderRequest oneOff(String orderId, long amount, String currency,
                                      String description, String customerEmail) {
        Order order = new Order(orderId, amount, currency, description, "auth", customerEmail, 0);
        PageCustomization branding = new PageCustomization("Solidgate QA", null, null);
        return new OrderRequest(order, branding);
    }
}
