package application.simulations;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.http.HttpDsl.http;

public class JsonPost extends SimulationBase {

	{
		HttpProtocolBuilder httpProtocolBuilder = http.baseUrl(SCHEME + "://" + HOST + ":" + PORT)
				.acceptHeader("text/plain")
				.acceptLanguageHeader("en-US,en;q=0.5")
				.acceptEncodingHeader("gzip, deflate")
				.userAgentHeader("Gatling");

		String body = "{\"id\":\"bclozel\",\"name\":\"brian clozel\"}";

		HttpRequestActionBuilder requestBuilder = http("jsonPost")
				.post("/user")
				.header("Content-Type", "application/json")
				.body(StringBody(body));


		if (HTTP2) {
			httpProtocolBuilder = httpProtocolBuilder.enableHttp2();
			requestBuilder = requestBuilder.resources(IntStream.range(0, H2_CONCURRENCY)
					.mapToObj(i -> http("req" + (i + 1))
							.post("/user")
							.header("Content-Type", "application/json")
							.body(StringBody(body)))
					.collect(Collectors.toList()));
		}

		setUp(httpProtocolBuilder, requestBuilder, "Post JSON and receive JSON payload");
	}
}
