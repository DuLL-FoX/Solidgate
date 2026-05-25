package qa.solidgate;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import qa.solidgate.api.ApiException;
import qa.solidgate.api.OrderRequest;
import qa.solidgate.api.OrderStatus;
import qa.solidgate.api.PaymentPage;
import qa.solidgate.api.SolidgateApi;
import qa.solidgate.ui.CheckoutPage;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentFlowTest {

    private static final long AMOUNT_MINOR_UNITS = 100L;
    private static final String CURRENCY = "USD";
    private static final String DESCRIPTION = "QA automation order";
    private static final String CUSTOMER_EMAIL = "qa+autotest@example.com";

    private static final Duration STATUS_POLL_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration STATUS_POLL_INTERVAL = Duration.ofSeconds(2);

    static {
        Configuration.browser = System.getProperty("selenide.browser", "chrome");
        Configuration.headless = Boolean.parseBoolean(System.getProperty("selenide.headless", "false"));
        Configuration.timeout = Duration.ofSeconds(20).toMillis();
        Configuration.pageLoadTimeout = Duration.ofSeconds(60).toMillis();
    }

    private final SolidgateApi api = new SolidgateApi(Config.get());

    private String orderId;
    private PaymentPage paymentPage;

    @BeforeAll
    void initOrder() {
        orderId = "qa-" + UUID.randomUUID();
        paymentPage = api.initPaymentPage(
                OrderRequest.oneOff(orderId, AMOUNT_MINOR_UNITS, CURRENCY, DESCRIPTION, CUSTOMER_EMAIL));
        assertThat(paymentPage.url()).as("payment page url").isNotBlank();
    }

    @Test
    @Order(1)
    @DisplayName("Pay order via payment page")
    void payOrderViaPaymentPage() {
        CheckoutPage checkout = CheckoutPage.open(paymentPage.url());
        checkout.pay(Config.get().successCard());
        checkout.awaitSuccess();
    }

    @Test
    @Order(2)
    @DisplayName("Status endpoint reflects successful payment")
    void statusEndpointReflectsSuccessfulPayment() {
        OrderStatus status = pollUntil(
                () -> api.orderStatus(orderId),
                OrderStatus::isApproved,
                STATUS_POLL_TIMEOUT,
                STATUS_POLL_INTERVAL);

        assertThat(status.order().amount()).isEqualTo(AMOUNT_MINOR_UNITS);
        assertThat(status.order().currency()).isEqualTo(CURRENCY);
        assertThat(status.isApproved())
                .as("order status should reflect a successful payment")
                .isTrue();
        assertThat(status.hasSuccessfulTransaction())
                .as("at least one transaction should be marked successful")
                .isTrue();
    }

    private static <T> T pollUntil(Supplier<T> source,
                                   Predicate<T> ready,
                                   Duration timeout,
                                   Duration interval) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        T last = null;
        ApiException lastError = null;
        while (true) {
            try {
                last = source.get();
                lastError = null;
                if (ready.test(last)) {
                    return last;
                }
            } catch (ApiException e) {
                lastError = e;
            }
            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            sleep(interval);
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new AssertionError("Condition not met within " + timeout + "; last value: " + last);
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
