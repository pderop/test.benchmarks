package application.simulations;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.gatling.javaapi.http.HttpDsl.http;

public class TextPlain extends SimulationBase {
	{
		HttpProtocolBuilder httpProtocolBuilder = http.baseUrl(SCHEME + "://" + HOST + ":" + PORT)
				.acceptHeader("text/plain")
				.acceptLanguageHeader("en-US,en;q=0.5")
				.acceptEncodingHeader("gzip, deflate")
				.userAgentHeader("Gatling");

		HttpRequestActionBuilder requestBuilder = http("plain_text")
				.get("/text");

		if (HTTP2) {
			httpProtocolBuilder = httpProtocolBuilder.enableHttp2();
			requestBuilder = requestBuilder.resources(IntStream.range(0, H2_CONCURRENCY)
					.mapToObj(i -> http("req" + (i + 1))
							.get("/text"))
					.collect(Collectors.toList()));
		}

		setUp(httpProtocolBuilder, requestBuilder, "Receive plain Text");
	}
}
