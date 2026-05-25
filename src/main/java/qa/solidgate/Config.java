package qa.solidgate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class Config {

    private static final String FILE = "/test.properties";
    private static final String DOTENV_FILE = ".env";
    private static final String ENV_PREFIX = "SOLIDGATE_";

    private static volatile Config instance;
    private static volatile Map<String, String> dotenv;

    private final String publicKey;
    private final String secretKey;
    private final String apiBaseUrl;
    private final String paymentPageBaseUrl;
    private final TestCard successCard;

    private Config(String publicKey, String secretKey, String apiBaseUrl,
                   String paymentPageBaseUrl, TestCard successCard) {
        this.publicKey = publicKey;
        this.secretKey = secretKey;
        this.apiBaseUrl = apiBaseUrl;
        this.paymentPageBaseUrl = paymentPageBaseUrl;
        this.successCard = successCard;
    }

    public static Config get() {
        Config local = instance;
        if (local == null) {
            synchronized (Config.class) {
                local = instance;
                if (local == null) {
                    local = load();
                    instance = local;
                }
            }
        }
        return local;
    }

    public String publicKey() {
        return publicKey;
    }

    public String secretKey() {
        return secretKey;
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    public String paymentPageBaseUrl() {
        return paymentPageBaseUrl;
    }

    public TestCard successCard() {
        return successCard;
    }

    private static Config load() {
        Properties props = readPropertiesFile();

        TestCard card = new TestCard(
                require("successCardNumber", props),
                require("successCardExpiry", props),
                require("successCardCvv", props),
                require("successCardHolder", props));

        return new Config(
                requireSecret("publicKey"),
                requireSecret("secretKey"),
                require("apiBaseUrl", props),
                require("paymentPageBaseUrl", props),
                card);
    }

    private static Properties readPropertiesFile() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException(FILE + " is missing on the classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props;
    }

    private static String requireSecret(String key) {
        String value = resolve(key, null);
        if (value == null) {
            throw new IllegalStateException(envName(key) + " is not set");
        }
        return value;
    }

    private static String require(String key, Properties props) {
        String value = resolve(key, props.getProperty(key));
        if (value == null) {
            throw new IllegalStateException("Property '" + key + "' is not set");
        }
        return value;
    }

    private static String resolve(String key, String fallback) {
        String env = System.getenv(envName(key));
        if (notBlank(env)) return env.trim();

        String sys = System.getProperty(key);
        if (notBlank(sys)) return sys.trim();

        String fromDotenv = dotenv().get(envName(key));
        if (notBlank(fromDotenv)) return fromDotenv.trim();

        return notBlank(fallback) ? fallback.trim() : null;
    }

    private static Map<String, String> dotenv() {
        Map<String, String> local = dotenv;
        if (local == null) {
            local = loadDotenv();
            dotenv = local;
        }
        return local;
    }

    private static Map<String, String> loadDotenv() {
        Path path = findDotenv();
        if (path == null) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String name = line.substring(0, eq).strip();
            String value = unquote(line.substring(eq + 1).strip());
            if (!name.isEmpty()) {
                values.put(name, value);
            }
        }
        return values;
    }

    private static Path findDotenv() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(DOTENV_FILE);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' || first == '\'') && first == last) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String envName(String camelCase) {
        StringBuilder sb = new StringBuilder(ENV_PREFIX);
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
