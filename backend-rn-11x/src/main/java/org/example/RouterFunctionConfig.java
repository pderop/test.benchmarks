package org.example;

import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Consumer;

final class RouterFunctionConfig {

	static Consumer<? super HttpServerRoutes> routesBuilder() {
		return r -> r.get("/text", hello());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> hello() {
		return (req, res) ->
				res.header("Content-Type", "text/plain")
						.sendObject(Unpooled.wrappedBuffer(HELLO_BYTES));
	}

	static final byte[] HELLO_BYTES = "Hello, World!".getBytes(StandardCharsets.UTF_8);
}