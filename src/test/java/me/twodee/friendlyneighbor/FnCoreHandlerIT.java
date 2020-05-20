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
import me.twodee.friendlyneighbor.component.FnCoreConfig;
import me.twodee.friendlyneighbor.configuration.LocationModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        Properties properties = new Properties();
        Injector injector = Guice.createInjector(new LocationModule(FnCoreConfig.createFromProperties(properties)));
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

    void storePostsInVicinity()
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

    void storePostsInAndOut()
    {
        fnCoreHandler.forwardRequestNearbyCustomLocation(FnCoreGenerated.PostData.newBuilder()
                                                                 .setPostId("x1")
                                                                 .setUserId("a")
                                                                 .setRadius(1)
                                                                 .setLocation(FnCoreGenerated.Location.newBuilder()
                                                                                      .setLatitude(72.00)
                                                                                      .setLongitude(20.00)
                                                                                      .build())
                                                                 .build());
        fnCoreHandler.forwardRequestNearbyCustomLocation(FnCoreGenerated.PostData.newBuilder()
                                                                 .setPostId("x2")
                                                                 .setRadius(600)
                                                                 .setLocation(FnCoreGenerated.Location.newBuilder()
                                                                                      .setLatitude(70.00)
                                                                                      .setLongitude(16.00)
                                                                                      .build())
                                                                 .setUserId("b")
                                                                 .build());
        fnCoreHandler.forwardRequestNearbyCustomLocation(FnCoreGenerated.PostData.newBuilder()
                                                                 .setPostId("x3")
                                                                 .setRadius(300)
                                                                 .setLocation(FnCoreGenerated.Location.newBuilder()
                                                                                      .setLongitude(73.203)
                                                                                      .setLatitude(22.878)
                                                                                      .build())
                                                                 .setUserId("c")
                                                                 .build());
    }

    @Test
    void fetchRequestsNearby_Successful()
    {
        registerUsers();
        storePostsInVicinity();

        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);

        assertTrue(usersNearby.getMetaResult().getSuccess());
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("postId").containsExactly("p3", "p2", "p1");
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("distance").isNotNull();
    }


    @Test
    void fetchRequestsNearby_Redundancy()
    {
        registerUsers();
        storePostsInVicinity();
        storePostsInVicinity();

        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);

        assertTrue(usersNearby.getMetaResult().getSuccess());
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("postId").containsExactly("p3", "p2", "p1");
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("distance").isNotNull();
    }

    @Test
    void fetchRequestsNearby_InvalidUid()
    {
        storePostsInVicinity();

        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);

        assertFalse(usersNearby.getMetaResult().getSuccess());
        assertTrue(usersNearby.getMetaResult().containsErrors("userId"));
    }

    @Test
    void fetchRequestsNearby_Empty()
    {
        registerUsers();

        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);

        assertTrue(usersNearby.getMetaResult().getSuccess());
        assertTrue(usersNearby.getRequestsList().isEmpty());
    }

    @Test
    void fetchRequestsNearby_DoesntContainPostsOutOfRadius()
    {
        registerUsers();
        storePostsInVicinity();
        storePostsInAndOut();

        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("postId").containsExactly("x3", "p3", "p2",
                                                                                                  "p1");
    }

    @Test
    void fetchRequestsNearby_DoesntContainOwnPosts()
    {
        registerUsers();
        storePostsInVicinity();
        storePostsInAndOut();

        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "a").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("postId").doesNotContain("p1");
    }

    @Test
    void getSingleUser_Successful()
    {
        registerUsers();
        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.LocationRadiusResult result = fnCoreHandler.getUserLocation(identifier);

        assertTrue(result.getMetaResult().getSuccess());
        assertThat(result.getLocation().getLatitude(), equalTo(22.878));
        assertThat(result.getLocation().getLongitude(), equalTo(73.205));
        assertThat(result.getRadius(), equalTo(150.0));
    }

    @Test
    void getSingleUser_InvalidId()
    {
        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.LocationRadiusResult result = fnCoreHandler.getUserLocation(identifier);

        assertFalse(result.getMetaResult().getSuccess());
        assertTrue(result.getMetaResult().containsErrors("userId"));
    }

    @Test
    void fanoutWithDefaultLocation_Success()
    {
        registerUsers();
        FnCoreGenerated.PostData postData = FnCoreGenerated.PostData.newBuilder()
                .setUserId("abc")
                .setPostId("test")
                .build();

        FnCoreGenerated.Result result = fnCoreHandler.forwardRequestNearbyDefaultLocation(postData);
        FnCoreGenerated.RequestsNearby requests = fnCoreHandler.fetchRequestsNearby(
                FnCoreGenerated.UserIdentifier.newBuilder().setUserId("a").build());
        assertTrue(result.getSuccess());
        Assertions.assertThat(requests.getRequestsList()).extracting("postId").contains("test");
    }

    @Test
    void fanoutWithDefaultLocation_PostWithExistingId()
    {
        registerUsers();
        storePostsInVicinity();
        FnCoreGenerated.PostData postData = FnCoreGenerated.PostData.newBuilder()
                .setUserId("abc")
                .setPostId("p1")
                .build();

        FnCoreGenerated.Result result = fnCoreHandler.forwardRequestNearbyDefaultLocation(postData);
        assertTrue(result.getSuccess());
    }

    @Test
    void fanoutWithDefaultLocation_InvalidIdentifier()
    {
        FnCoreGenerated.PostData postData = FnCoreGenerated.PostData.newBuilder()
                .setUserId("abc")
                .setPostId("test")
                .build();

        FnCoreGenerated.Result result = fnCoreHandler.forwardRequestNearbyDefaultLocation(postData);
        assertFalse(result.getSuccess());
        assertTrue(result.containsErrors("userId"));
    }


    @Test
    void fanoutWithCustomLocation_InvalidIdentifier()
    {
        FnCoreGenerated.PostData postData = FnCoreGenerated.PostData.newBuilder()
                .setPostId("test")
                .setRadius(300)
                .setLocation(FnCoreGenerated.Location.newBuilder()
                                     .setLongitude(73.203)
                                     .setLatitude(22.878)
                                     .build())
                .setUserId("INVALID")
                .build();

        FnCoreGenerated.Result result = fnCoreHandler.forwardRequestNearbyCustomLocation(postData);
        assertFalse(result.getSuccess());
        assertTrue(result.containsErrors("userId"));
    }

    @Test
    void fanoutWithCustomLocation_Successful()
    {
        registerUsers();
        FnCoreGenerated.PostData postData = FnCoreGenerated.PostData.newBuilder()
                .setPostId("test")
                .setRadius(300)
                .setLocation(FnCoreGenerated.Location.newBuilder()
                                     .setLongitude(73.203)
                                     .setLatitude(22.878)
                                     .build())
                .setUserId("abc")
                .build();

        FnCoreGenerated.Result result = fnCoreHandler.forwardRequestNearbyCustomLocation(postData);
        FnCoreGenerated.RequestsNearby requests = fnCoreHandler.fetchRequestsNearby(
                FnCoreGenerated.UserIdentifier.newBuilder()
                        .setUserId("a")
                        .build());
        assertTrue(result.getSuccess());
        Assertions.assertThat(requests.getRequestsList()).extracting("postId").contains("test");
    }

    @Test
    void fanoutWithCustomLocation_NoLocationSet()
    {
        registerUsers();
        FnCoreGenerated.PostData postData = FnCoreGenerated.PostData.newBuilder()
                .setPostId("test")
                .setLocation(FnCoreGenerated.Location.newBuilder()
                                     .setLongitude(73.203)
                                     .setLatitude(22.878)
                                     .build())
                .setUserId("abc")
                .build();

        FnCoreGenerated.Result result = fnCoreHandler.forwardRequestNearbyCustomLocation(postData);
        assertFalse(result.getSuccess());
        assertTrue(result.getErrorsMap().containsKey("location"));
    }

    @Test
    void fanoutWithCustomLocation_NoRadiusSet()
    {
        registerUsers();
        FnCoreGenerated.PostData postData = FnCoreGenerated.PostData.newBuilder()
                .setPostId("test")
                .setRadius(300)
                .setUserId("abc")
                .build();

        FnCoreGenerated.Result result = fnCoreHandler.forwardRequestNearbyCustomLocation(postData);
        assertFalse(result.getSuccess());
        assertTrue(result.getErrorsMap().containsKey("location"));
    }

    @Test
    void deletePostTest()
    {
        registerUsers();
        storePostsInVicinity();

        FnCoreGenerated.Result result = fnCoreHandler.deleteRequest(FnCoreGenerated.PostData.newBuilder()
                                                                            .setPostId("p1")
                                                                            .build());
        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder().setUserId(
                "abc").build();
        FnCoreGenerated.RequestsNearby usersNearby = fnCoreHandler.fetchRequestsNearby(identifier);

        assertTrue(result.getSuccess());
        Assertions.assertThat(usersNearby.getRequestsList()).extracting("postId").containsExactly("p3", "p2");

    }
}