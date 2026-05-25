package qa.solidgate.api;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

public final class Signer {

    private static final String ALGORITHM = "HmacSHA512";

    private final String publicKey;
    private final byte[] secret;

    public Signer(String publicKey, String secretKey) {
        this.publicKey = publicKey;
        this.secret = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            String payload = publicKey + body + publicKey;
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(digest);
            return Base64.getEncoder().encodeToString(hex.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot compute Solidgate signature", e);
        }
    }
}
