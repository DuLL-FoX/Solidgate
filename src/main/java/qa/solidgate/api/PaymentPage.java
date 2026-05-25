package qa.solidgate.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentPage(
        @JsonProperty("url") String url,
        @JsonProperty("id") String pageId) {
}
