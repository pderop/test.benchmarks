package org.example;

import io.netty5.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import reactor.netty5.http.Http11SslContextSpec;
import reactor.netty5.http.Http2SslContextSpec;
import reactor.netty5.http.HttpProtocol;
import reactor.netty5.http.client.Http2AllocationStrategy;
import reactor.netty5.http.client.HttpClient;
import reactor.netty5.http.server.HttpServer;
import reactor.netty5.resources.ConnectionProvider;

import java.security.cert.CertificateException;
import java.util.function.Function;

public class ServerApplication {
    static final String HOST = System.getProperty("HOST", "0");
    static final String PROTOCOL = System.getProperty("PROTOCOL", "H1");
    static final int PORT = Integer.parseInt(System.getProperty("PORT", "8080"));
    static final String BACKEND_HOST = System.getProperty("BACKEND_HOST", "127.0.0.1");
    static final String BACKEND_PORT = System.getProperty("BACKEND_PORT", "8090");
    static final int H2_MAX_CONNECTION = Integer.getInteger("H2_MAXCONN", Runtime.getRuntime().availableProcessors());
    static final int H2_MAX_STREAMS = Integer.getInteger("H2_MAXSTREAMS", 100);
    static final ConnectionProvider PROVIDER = configure(ConnectionProvider.builder("gateway"));
    static final HttpClient CLIENT = configure(HttpClient.create(PROVIDER));

    static ConnectionProvider configure(ConnectionProvider.Builder builder) {
        return switch (PROTOCOL) {
            case "H2" -> builder.allocationStrategy(
                            Http2AllocationStrategy.builder()
                                    .minConnections(1)
                                    .maxConnections(H2_MAX_CONNECTION)
                                    .maxConcurrentStreams(H2_MAX_STREAMS)
                                    .build())
                    .pendingAcquireMaxCount(8 * 500)
                    .build();
            case "H1", "H1S" -> builder.maxConnections(500)
                    .pendingAcquireMaxCount(8 * 500)
                    .build();
            default -> throw new IllegalArgumentException("Invalid protocol: " + PROTOCOL);

        };
    }

    static HttpClient configure(HttpClient client) {
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

            return switch (PROTOCOL) {
                case "H1" -> server
                        .protocol(HttpProtocol.HTTP11);

                case "H1S" -> server
                        .secure(spec -> spec.sslContext(Http11SslContextSpec.forServer(cert.certificate(), cert.privateKey())))
                        .protocol(HttpProtocol.HTTP11);

                case "H2" -> server
                        .secure(spec -> spec.sslContext(Http2SslContextSpec.forServer(cert.certificate(), cert.privateKey())))
                        .protocol(HttpProtocol.H2)
                        .http2Settings(builder -> builder.maxConcurrentStreams(H2_MAX_STREAMS));

                default -> throw new IllegalArgumentException("Invalid protocol: " + PROTOCOL);
            };
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println("Server starting (protocol=" + PROTOCOL + ").");
        configure(HttpServer.create().host(HOST).port(PORT))
                .metrics(true, Function.identity())
                .route(RouterFunctionConfig.routesBuilder())
                .doOnBound(server -> System.out.println("Server is bound on " + server.address() + " using protocol " + PROTOCOL))
                .bindNow()
                .onDispose()
                .block();
    }
}
