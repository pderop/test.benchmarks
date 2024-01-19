package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.buffer.DefaultBufferAllocators;
import io.netty5.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.netty5.http.server.HttpServerRequest;
import reactor.netty5.http.server.HttpServerResponse;
import reactor.netty5.http.server.HttpServerRoutes;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.example.ServerApplication.CLIENT;

final class RouterFunctionConfig {

	static Consumer<? super HttpServerRoutes> routesBuilder() {
		return r -> r.get("/text", hello())
				.get("/remote", remote())
				.post("/echo", echo())
				.get("/home", home())
				.post("/user", createUser())
				.get("/user/{id}", findUser());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> hello() {
		return (req, res) ->
				res.header("Content-Type", "text/plain")
						.sendObject(msgSupplier.get());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> remote() {
		return (req, res) ->
				res.header("Content-Type", "text/plain")
						.send(CLIENT.get()
								.uri("/text")
								.responseContent()
								.transferOwnership());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> echo() {
		return (req, res) -> res.send(req.receive().transferOwnership().next());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> home() {
		return (req, res) -> res.sendFile(RESOURCE);
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> createUser() {
		return (req, res) ->
				res.status(HttpResponseStatus.CREATED)
						.header("Content-Type", "application/json")
						.sendObject(
								req.receive()
										.aggregate()
										.asInputStream()
										.map(in -> toByteBuf(fromInputStream(in))));
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> findUser() {
		return (req, res) ->
				res.header("Content-Type", "application/json")
						.sendObject(toByteBuf(new User(req.param("id"), "Ben Chmark")));
	}

	static Buffer toByteBuf(Object any) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			mapper.writeValue(out, any);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return DefaultBufferAllocators.preferredAllocator().copyOf(out.toByteArray());
	}

	static Object fromInputStream(InputStream in) {
		try {
			return mapper.readValue(in, User.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static final byte[] HELLO_BYTES = "Hello, World!".getBytes(StandardCharsets.UTF_8);

	static final ObjectMapper mapper = new ObjectMapper();

	static final Path RESOURCE;
	static final byte[] msgBytes = "Hello, World!".getBytes(StandardCharsets.ISO_8859_1);
	static final Supplier<Buffer> msgSupplier = BufferAllocator.onHeapUnpooled().constBufferSupplier(msgBytes);
	static final byte[] exitBytes = "Exiting!".getBytes(StandardCharsets.ISO_8859_1);
	static final Supplier<Buffer> exitSupplier = BufferAllocator.onHeapUnpooled().constBufferSupplier(exitBytes);

	static Path resource;
	static {
		Path tempFile = null;
		try {
			String template = """
					<!DOCTYPE html>
					<html lang="en">
					<head>
					    <meta charset="UTF-8">
					    <title>Home</title>
					</head>
					<body>
					    <p data-th-text="Hello, World!">Message</p>
					</body>
					</html>""";
			tempFile = Files.createTempFile("reactor-netty", null);
			tempFile.toFile().deleteOnExit();
			Files.writeString(tempFile, template);
		} catch (Exception e) {
			e.printStackTrace();
		}
		RESOURCE = tempFile;
	}
    
}
