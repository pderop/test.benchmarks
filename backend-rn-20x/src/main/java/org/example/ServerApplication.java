package org.example;

import io.netty5.handler.ssl.util.SelfSignedCertificate;
import reactor.netty5.http.Http11SslContextSpec;
import reactor.netty5.http.Http2SslContextSpec;
import reactor.netty5.http.HttpProtocol;
import reactor.netty5.http.server.HttpServer;

import java.security.cert.CertificateException;

public class ServerApplication {
	static final String HOST = System.getProperty("HOST", "0");
	static final String PROTOCOL = System.getProperty("PROTOCOL", "H1");
	static final int PORT = Integer.parseInt(System.getProperty("PORT", "8090"));
	static final int H2_MAX_STREAMS = Integer.getInteger("H2_MAXSTREAMS", 100);

	public static void main(String[] args) {
		configure(HttpServer.create().host(HOST).port(PORT))
				.route(RouterFunctionConfig.routesBuilder())
				.doOnBound(server -> System.out.println("Server is bound on " + server.address() + " using protocol " + PROTOCOL))
				.bindNow()
				.onDispose()
				.block();
	}

	static HttpServer configure(HttpServer server) {
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate();

			return switch (PROTOCOL) {
				case "H1" -> server
						.protocol(HttpProtocol.HTTP11);

				case "H1S" -> server
						.secure(spec -> spec.sslContext(Http11SslContextSpec.forServer(cert.certificate(), cert.privateKey())))
						.protocol(HttpProtocol.HTTP11);

				case "H2" -> server
						.secure(spec -> spec.sslContext(Http2SslContextSpec.forServer(cert.certificate(), cert.privateKey())))
						.http2Settings(builder -> builder.maxConcurrentStreams(H2_MAX_STREAMS))
						.protocol(HttpProtocol.H2);

				default -> throw new IllegalArgumentException("Invalid protocol: " + PROTOCOL);
			};
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}
}
