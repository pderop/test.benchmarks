package org.example;

import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import org.reactivestreams.Publisher;
import reactor.netty5.http.server.HttpServerRequest;
import reactor.netty5.http.server.HttpServerResponse;
import reactor.netty5.http.server.HttpServerRoutes;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class RouterFunctionConfig {

	static Consumer<? super HttpServerRoutes> routesBuilder() {
		return r -> r.get("/text", hello());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> hello() {
		return (req, res) ->
				res.header("Content-Type", "text/plain")
						.sendObject(msgSupplier.get());
	}

	static final byte[] msgBytes = "Hello, World!".getBytes(StandardCharsets.ISO_8859_1);
	static final Supplier<Buffer> msgSupplier = BufferAllocator.onHeapUnpooled().constBufferSupplier(msgBytes);
}