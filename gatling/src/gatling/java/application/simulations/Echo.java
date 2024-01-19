package application.simulations;

import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.gatling.javaapi.http.HttpDsl.http;


public class Echo extends SimulationBase {
	static final String BODY = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer at orci mollis, accumsan ligula elementum, auctor dui. Donec eleifend risus vel turpis egestas, vitae pretium metus tincidunt. Proin aliquet ipsum vel dolor rutrum semper. Morbi et sem eu lorem egestas ullamcorper non a massa.";

	{
		HttpProtocolBuilder httpProtocolBuilder = http.baseUrl(SCHEME + "://" + HOST + ":" + PORT)
				.acceptHeader("text/plain")
				.acceptLanguageHeader("en-US,en;q=0.5")
				.acceptEncodingHeader("gzip, deflate")
				.userAgentHeader("Gatling");

		HttpRequestActionBuilder requestBuilder = http("echo")
				.post("/echo")
				.header("Content-Type", "text/plain")
				.body(CoreDsl.StringBody(BODY));

		if (HTTP2) {
			httpProtocolBuilder = httpProtocolBuilder.enableHttp2();
			requestBuilder = requestBuilder.resources(IntStream.range(0, H2_CONCURRENCY)
					.mapToObj(i -> http("req" + (i + 1))
							.post("/echo")
							.body(CoreDsl.StringBody(BODY)))
					.collect(Collectors.toList()));
		}

		setUp(httpProtocolBuilder, requestBuilder, "Send text to an echo endpoint");
	}
}
