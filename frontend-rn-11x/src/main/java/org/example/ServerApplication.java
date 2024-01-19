package org.example;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.Http2AllocationStrategy;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.ConnectionProvider;

import java.security.cert.CertificateException;
import java.util.function.Function;

public class ServerApplication {
    static final String HOST = System.getProperty("HOST", "0");
    static final String PROTOCOL = System.getProperty("PROTOCOL", "H2");
    static final int PORT = Integer.parseInt(System.getProperty("PORT", "8080"));
    static final String BACKEND_HOST = System.getProperty("BACKEND_HOST", "127.0.0.1");
    static final String BACKEND_PORT = System.getProperty("BACKEND_PORT", "8090");
    static final boolean HTTP2 = PROTOCOL.equals("H2") || PROTOCOL.equals("H2C");
    static final int H2_MAX_CONNECTION = Integer.getInteger("H2_MAXCONN", Runtime.getRuntime().availableProcessors());
    static final int H2_MAX_STREAMS = Integer.getInteger("H2_MAXSTREAMS", 100);
    static final ConnectionProvider PROVIDER = configure(ConnectionProvider.builder("http"));
    static final HttpClient CLIENT = configure(HttpClient.create(PROVIDER));

    static ConnectionProvider configure(ConnectionProvider.Builder builder) {
        builder = builder.maxConnections(500)
                .pendingAcquireMaxCount(8 * 500);

        if (HTTP2) {
            builder = builder.allocationStrategy(Http2AllocationStrategy.builder()
                    .minConnections(1)
                    .maxConnections(H2_MAX_CONNECTION)
                    .maxConcurrentStreams(H2_MAX_STREAMS).build());
        }
        return builder.build();
    }

    static HttpClient configure(HttpClient client) {
        client = client.metrics(true, Function.identity());
        return switch (PROTOCOL) {
            case "H1" -> client
                    .protocol(HttpProtocol.HTTP11)
                    .baseUrl("http://" + BACKEND_HOST + ":" + BACKEND_PORT);

            case "H1S" -> client
                    .protocol(HttpProtocol.HTTP11)
                    .secure(spec -> spec.sslContext(Http11SslContextSpec.forClient().configure(builder -> builder.trustManager(InsecureTrustManagerFactory.INSTANCE))))
                    .baseUrl("https://" + BACKEND_HOST + ":" + BACKEND_PORT);

            case "H2" -> client
                    .protocol(HttpProtocol.H2)
                    .secure(spec -> spec.sslContext(Http2SslContextSpec.forClient().configure(builder -> builder.trustManager(InsecureTrustManagerFactory.INSTANCE))))
                    .baseUrl("https://" + BACKEND_HOST + ":" + BACKEND_PORT);

            default -> throw new IllegalArgumentException("Invalid protocol: " + PROTOCOL);
        };
    }

    static HttpServer configure(HttpServer server) {
        try {
            SelfSignedCertificate cert = new SelfSignedCertificate();
            server = server.metrics(true, Function.identity());

            return switch (PROTOCOL) {
                case "H1" -> server
                        .protocol(HttpProtocol.HTTP11);

                case "H1S" -> server
                        .secure(spec -> spec.sslContext(Http11SslContextSpec.forServer(cert.certificate(), cert.privateKey())))
                        .protocol(HttpProtocol.HTTP11);

                case "H2" -> server
                        .secure(spec -> spec.sslContext(Http2SslContextSpec.forServer(cert.certificate(), cert.privateKey())))
                        .protocol(HttpProtocol.H2);

                default -> throw new IllegalArgumentException("Invalid protocol: " + PROTOCOL);
            };
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        configure(HttpServer.create().host(HOST).port(PORT))
                .route(RouterFunctionConfig.routesBuilder())
                .doOnBound(server -> System.out.println("Frontend bound on " + server.address() +
                        ", protocol=" + PROTOCOL + ", backend host=" + BACKEND_HOST + ", backend port=" + BACKEND_PORT))
                .bindNow()
                .onDispose()
                .block();
    }
}
