package me.twodee.friendlyneighbor;

import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * InProcess server implementation for testing FnCoreHandler
 */
public class InProcessServer
{
    private static final Logger logger = LoggerFactory.getLogger(InProcessServer.class);

    private Server server;

    public <T extends io.grpc.BindableService> void start(T service) throws IOException, InstantiationException, IllegalAccessException
    {
        server = InProcessServerBuilder
                .forName("test")
                .directExecutor()
                .addService(service)
                .build()
                .start();
        logger.info("InProcessServer started.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            InProcessServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    void stop()
    {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException
    {
        if (server != null) {
            server.awaitTermination();
        }
    }
}