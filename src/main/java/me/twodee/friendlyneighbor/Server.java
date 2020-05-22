package me.twodee.friendlyneighbor;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Server
{
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final int port;
    private final io.grpc.Server server;

    public Server(int port, BindableService service)
    {
        this.port = port;
        server = ServerBuilder.forPort(port).addService(service).build();
    }
    /** Start serving requests. */
    public void start() throws IOException, InterruptedException
    {
        server.start();
        logger.info("Server started, listening on " + port);
        server.awaitTermination();
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        logger.info("Server shutting down");
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
}
