package org.example;

import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.util.retry.Retry;

import java.time.Duration;

public class ServerApplication {
    static final String HOST = System.getProperty("HOST", "0");
    static final int PORT = Integer.parseInt(System.getProperty("PORT", "8080"));
    static final String BACKEND_HOST = System.getProperty("BACKEND_HOST", "127.0.0.1");
    static final int BACKEND_PORT = Integer.getInteger("BACKEND_PORT", 8090);

    static final RSocket SOURCE =
            RSocketConnector.create()
                    .reconnect(Retry.backoff(10, Duration.ofMillis(1000)))
                    .connect(TcpClientTransport.create(BACKEND_HOST, BACKEND_PORT))
                    .block();

    final static RSocketClient CLIENT = RSocketClient.from(SOURCE);

    static HttpServer configure(HttpServer server) {
        return server.protocol(HttpProtocol.HTTP11);
    }

    public static void main(String[] args) {
        System.out.println("Server starting");
        configure(HttpServer.create().host(HOST).port(PORT))
                .protocol(HttpProtocol.HTTP11)
                .route(RouterFunctionConfig.routesBuilder())
                .doOnBound(server -> System.out.println("Server is bound on " + server.address()))
                .bindNow()
                .onDispose()
                .block();
    }
}
