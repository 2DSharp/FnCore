package me.twodee.friendlyneighbor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import me.twodee.friendlyneighbor.configuration.LocationModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FnCoreHandlerIT
{
    private InProcessServer inProcessServer;
    private ManagedChannel channel;
    private FnCoreGrpc.FnCoreBlockingStub fnCoreHandler;

    private MongodExecutable mongodExecutable;
    private MongoTemplate mongoTemplate;
    private RedisServer redisServer;
    private JedisPool jedisPool;

    @BeforeEach
    void setUp() throws IllegalAccessException, IOException, InstantiationException
    {
        // The order here is very important
        startMongo();
        startRedis();
        startGrpc();
    }

    void startGrpc() throws IllegalAccessException, IOException, InstantiationException
    {
        inProcessServer = new InProcessServer();

        Injector injector = Guice.createInjector(new LocationModule());
        inProcessServer.start(injector.getInstance(FnCoreHandler.class));
        channel = InProcessChannelBuilder
                .forName("test")
                .directExecutor()
                .usePlaintext()
                .build();
        fnCoreHandler = FnCoreGrpc.newBlockingStub(channel);
    }

    void startMongo() throws IOException
    {
        String ip = "localhost";
        int port = 27017;

        IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION)
                .net(new Net(ip, port, Network.localhostIsIPv6()))
                .build();

        MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();
        mongoTemplate = new MongoTemplate(MongoClients.create(), "test");
    }

    void startRedis() throws IOException
    {
        redisServer = new RedisServer(6379);
        redisServer.start();
        jedisPool = new JedisPool("localhost", 6379);

    }

    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @AfterEach
    void cleanUp()
    {
        channel.shutdownNow();
        inProcessServer.stop();
        mongodExecutable.stop();
        redisServer.stop();
    }

    void registerUsers()
    {
        fnCoreHandler.saveUserLocation(FnCoreGenerated.RegistrationRequest.newBuilder()
                                               .setUserId("abc")
                                               .setLocation(
                                                       FnCoreGenerated.Location.newBuilder()
                                                               .setLongitude(73.205)
                                                               .setLatitude(22.878)
                                                               .build()
                                               )
                                               .setRadius(150)
                                               .build());

        fnCoreHandler.saveUserLocation(FnCoreGenerated.RegistrationRequest.newBuilder()
                                               .setUserId("a")
                                               .setLocation(
                                                       FnCoreGenerated.Location.newBuilder()
                                                               .setLongitude(73.203)
                                                               .setLatitude(22.878)
                                                               .build()
                                               )
                                               .setRadius(150)
                                               .build());
        fnCoreHandler.saveUserLocation(FnCoreGenerated.RegistrationRequest.newBuilder()
                                               .setUserId("b")
                                               .setLocation(
                                                       FnCoreGenerated.Location.newBuilder()
                                                               .setLongitude(73.202)
                                                               .setLatitude(22.874)
                                                               .build()
                                               )
                                               .setRadius(150)
                                               .build());
        fnCoreHandler.saveUserLocation(FnCoreGenerated.RegistrationRequest.newBuilder()
                                               .setUserId("c")
                                               .setLocation(
                                                       FnCoreGenerated.Location.newBuilder()
                                                               .setLongitude(73.203)
                                                               .setLatitude(22.878)
                                                               .build()
                                               )
                                               .setRadius(150)
                                               .build());

        fnCoreHandler.saveUserLocation(FnCoreGenerated.RegistrationRequest.newBuilder()
                                               .setUserId("d")
                                               .setLocation(
                                                       FnCoreGenerated.Location.newBuilder()
                                                               .setLongitude(37.203)
                                                               .setLatitude(20.878)
                                                               .build()
                                               )
                                               .setRadius(150)
                                               .build());
    }

    void storePosts()
    {
        fnCoreHandler.forwardRequestNearbyDefaultLocation(FnCoreGenerated.PostData.newBuilder()
                                                                  .setPostId("p1")
                                                                  .setUserId("a")
                                                                  .build());
        fnCoreHandler.forwardRequestNearbyDefaultLocation(FnCoreGenerated.PostData.newBuilder()
                                                                  .setPostId("p2")
                                                                  .setUserId("b")
                                                                  .build());
        fnCoreHandler.forwardRequestNearbyDefaultLocation(FnCoreGenerated.PostData.newBuilder()
                                                                  .setPostId("p3")
                                                                  .setUserId("c")
                                                                  .build());
    }

    @Test
    void fetchRequestsNearby_Successful()
    {
        registerUsers();
        storePosts();

        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);

        assertTrue(usersNearby.getMetaResult().getSuccess());
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("postId").containsExactly("p3", "p2", "p1");
    }
}