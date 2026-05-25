package qa.solidgate.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import qa.solidgate.TestCard;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;

public final class CheckoutPage {

    private static final Duration PAY_TIMEOUT = Duration.ofSeconds(60);

    private static final String CARD_NUMBER = "input[name='cardNumber'], input[name='card_number']";
    private static final String CARD_EXPIRY = "input[name='cardExpiryDate'], input[name='card_exp_date']";
    private static final String CARD_CVV    = "input[name='cardCvv'], input[name='card_cvv']";
    private static final String CARD_HOLDER = "input[name='cardHolder'], input[name='card_holder']";
    private static final String SUBMIT      = "button[type='submit']";

    private static final String PAYMENT_IFRAME =
            "iframe[src*='solidgate'], iframe[src*='payment'], iframe[name*='pay'], iframe[id*='pay']";

    private static final String SUCCESS_MARKERS =
            "(?iu)(payment\\s+(is\\s+)?successful|successful\\s+payment|успеш|успіш)";

    private CheckoutPage() {
    }

    public static CheckoutPage open(String url) {
        Selenide.open(url);
        return new CheckoutPage();
    }

    public void pay(TestCard card) {
        // PCI-compliant Solidgate checkouts render card fields inside an iframe.
        // Fall back to switching into it if fields are not present in the top frame.
        boolean switched = switchIntoPaymentFrameIfNeeded();
        try {
            $(CARD_NUMBER).shouldBe(Condition.interactable, PAY_TIMEOUT).setValue(card.number());
            $(CARD_EXPIRY).shouldBe(Condition.interactable, PAY_TIMEOUT).setValue(card.expiry());
            $(CARD_CVV).shouldBe(Condition.interactable, PAY_TIMEOUT).setValue(card.cvv());

            SelenideElement holder = $(CARD_HOLDER);
            if (holder.is(Condition.interactable)) {
                holder.setValue(card.holder());
            }

            $(SUBMIT).shouldBe(Condition.enabled, PAY_TIMEOUT).click();
        } finally {
            if (switched) {
                Selenide.switchTo().defaultContent();
            }
        }
    }

    public void awaitSuccess() {
        $("body").shouldHave(Condition.matchText(SUCCESS_MARKERS), PAY_TIMEOUT);
    }

    private static boolean switchIntoPaymentFrameIfNeeded() {
        if ($(CARD_NUMBER).exists()) {
            return false;
        }
        SelenideElement frame = $(PAYMENT_IFRAME);
        if (!frame.exists()) {
            return false;
        }
        Selenide.switchTo().frame(frame);
        return true;
    }
}
