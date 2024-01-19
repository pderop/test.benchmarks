package org.example;

import io.netty.buffer.Unpooled;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.example.ServerApplication.CLIENT;

final class RouterFunctionConfig {

	static Consumer<? super HttpServerRoutes> routesBuilder() {
		return r -> r.get("/text", hello())
				.get("/remote", remote());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> hello() {
		return (req, res) ->
				res.header("Content-Type", "text/plain")
						.sendObject(Unpooled.wrappedBuffer(HELLO_BYTES));
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> remote() {
		return (req, res) ->
				res.header("Content-Type", "text/plain")
						.send(CLIENT.requestResponse(Mono.just(DefaultPayload.create("Echo")))
								.map(payload ->  payload.retain().data()));
	}

	static final byte[] HELLO_BYTES = "Hello, World!".getBytes(StandardCharsets.UTF_8);
}