package org.example;

import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import io.rsocket.*;

public class ServerApplication {
	static final String HOST = System.getProperty("HOST", "0");
	static final String PROTOCOL = System.getProperty("PROTOCOL", "TCP");
	static final int PORT = Integer.parseInt(System.getProperty("PORT", "8090"));


	public static void main(String[] args) throws InterruptedException {
		System.out.println("Starting RSocket server on port " + PORT);

		RSocketServer.create(
						SocketAcceptor.forRequestResponse(
								p -> {
									String data = p.getDataUtf8();
									Payload responsePayload = DefaultPayload.create("Echo: " + data);
									p.release();
									return Mono.just(responsePayload);
								}))
				.bind(TcpServerTransport.create(HOST, PORT))
				.block();

		Thread.sleep(Integer.MAX_VALUE);
	}

}
