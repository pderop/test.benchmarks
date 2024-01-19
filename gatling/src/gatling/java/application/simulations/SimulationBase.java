package application.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.time.Duration;
import java.util.regex.Pattern;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.incrementConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;

public abstract class SimulationBase extends Simulation {

    public static final String HOST = System.getProperty("HOST", "127.0.0.1");
    public static final int PORT = Integer.parseInt(System.getProperty("PORT", "8080"));
    public static final String PROTOCOL = System.getProperty("PROTOCOL", "H2");
    public static final Duration DURATION = parseDuration(System.getProperty("DURATION", "60"));
    public static final boolean HTTP2 = PROTOCOL.equals("H2");
    public static final int INCREMENT = Integer.getInteger("INCREMENT", 100);
    public static final int STEPS = Integer.getInteger("STEPS", 10);
    public static final Duration LEVEL_LASTING = parseDuration(System.getProperty("LEVEL_LASTING", "1"));
    public static final Duration RAMP_LASTING = parseDuration(System.getProperty("RAMP_LASTING", "1"));
    public static final String SCHEME = (PROTOCOL.equals("H1S") || PROTOCOL.equals("H2")) ? "https" : "http";
    public static final int H2_CONCURRENCY = Integer.getInteger("H2_CONCURRENCY", 10);

    protected void setUp(HttpProtocolBuilder httpProtocolBuilder, HttpRequestActionBuilder requestBuilder, String scnName) {
        ScenarioBuilder scn = scenario(scnName)
                .forever().on(exec(requestBuilder));

        setUp(scn
                .injectClosed(
                        incrementConcurrentUsers(INCREMENT)
                                .times(STEPS)
                                .eachLevelLasting(LEVEL_LASTING)
                                .separatedByRampsLasting(RAMP_LASTING)
                                .startingFrom(0)))
                .maxDuration(DURATION)
                .protocols(httpProtocolBuilder);
    }

    public static Duration parseDuration(String durationProperty) {
        boolean endsWithSMH = Pattern.compile("(m|s|h)$", Pattern.CASE_INSENSITIVE).matcher(durationProperty).find();
        if (endsWithSMH) {
            String unit = durationProperty.substring(durationProperty.length() - 1);
            String value = durationProperty.substring(0, durationProperty.length() - 1);

            try {
                long amount = Long.parseLong(value);
                switch (unit.toLowerCase()) {
                    case "s":
                        return Duration.ofSeconds(amount);
                    case "m":
                        return Duration.ofMinutes(amount);
                    case "h":
                        return Duration.ofHours(amount);
                    default:
                        // Default: Duration in secs
                        return Duration.ofSeconds(amount);
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        } else {
            return Duration.ofSeconds(Long.parseLong(durationProperty));
        }
    }
}
