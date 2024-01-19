package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
						.sendObject(Unpooled.wrappedBuffer(HELLO_BYTES));
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> remote() {
		return (req, res) ->
				res.header("Content-Type", "text/plain")
						.send(CLIENT.get()
								.uri("/text")
								.responseContent()
								.retain());
	}

	static BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> echo() {
		return (req, res) -> res.send(req.receive().retain().next());
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

	static ByteBuf toByteBuf(Object any) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			MAPPER.writeValue(out, any);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return ByteBufAllocator.DEFAULT
				.buffer()
				.writeBytes(out.toByteArray());
	}

	static Object fromInputStream(InputStream in) {
		try {
			return MAPPER.readValue(in, User.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static final byte[] HELLO_BYTES = "Hello, World!".getBytes(StandardCharsets.UTF_8);

	static final ObjectMapper MAPPER = new ObjectMapper();

	static final Path RESOURCE;

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