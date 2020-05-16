package me.twodee.friendlyneighbor.repository;

import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridPostRepositoryIT
{
    private MongodExecutable mongodExecutable;
    private MongoTemplate template;
    private RedisServer redisServer;
    private JedisPool jedisPool;
    private static final String FEED_NAMESPACE = "FN_CORE.FEED";


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
        template = new MongoTemplate(MongoClients.create(), "test");
    }

    void startRedis() throws IOException
    {
        redisServer = new RedisServer(6379);
        redisServer.start();
        jedisPool = new JedisPool("localhost", 6379);

    }

    @BeforeEach
    void setUp() throws IOException
    {
        startMongo();
        startRedis();
    }

    @AfterEach
    void cleanUp()
    {
        mongodExecutable.stop();
        redisServer.stop();
    }

    @Test
    void testFanoutOutOfCache()
    {
        HybridPostRepository repository = new HybridPostRepository(template, jedisPool);
        List<UserLocation> nearbyUsers = new ArrayList<>();
        nearbyUsers.add(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        nearbyUsers.add(new UserLocation("xyz", new UserLocation.Position(22.507449, 88.32), 2100));

        UserLocation currentUserLocation = new UserLocation("test", new UserLocation.Position(22.72, 87.34),
                                                            1100);
        repository.forwardToUsers(nearbyUsers, new Post("a", currentUserLocation, LocalDateTime.now()));

        try (Jedis jedis = jedisPool.getResource()) {
            assertTrue(jedis.exists(getKey("abc123")));
            assertTrue(jedis.exists(getKey("xyz")));

            assertThat(jedis.lpop(getKey("abc123")), equalTo("a"));
            assertThat(jedis.lpop(getKey("xyz")), equalTo("a"));
        }
    }

    @Test
    void testFanoutInCache_PostOnTopOfList()
    {
        HybridPostRepository repository = new HybridPostRepository(template, jedisPool);
        List<UserLocation> nearbyUsers = new ArrayList<>();
        nearbyUsers.add(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        nearbyUsers.add(new UserLocation("xyz", new UserLocation.Position(22.507449, 88.32), 2100));

        UserLocation currentUserLocation = new UserLocation("test", new UserLocation.Position(22.72, 87.34),
                                                            1100);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lpush(getKey("abc123"), "t1", "t2");

            repository.forwardToUsers(nearbyUsers, new Post("a", currentUserLocation, LocalDateTime.now()));

            assertTrue(jedis.exists(getKey("abc123")));
            assertTrue(jedis.exists(getKey("xyz")));
            assertThat(jedis.lpop(getKey("abc123")), equalTo("a"));
        }
    }

    @Test
    void testFanoutInCache_ListHasBeenUpdated()
    {
        HybridPostRepository repository = new HybridPostRepository(template, jedisPool);
        List<UserLocation> nearbyUsers = new ArrayList<>();
        nearbyUsers.add(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        nearbyUsers.add(new UserLocation("xyz", new UserLocation.Position(22.507449, 88.32), 2100));

        UserLocation currentUserLocation = new UserLocation("test", new UserLocation.Position(22.72, 87.34),
                                                            1100);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lpush(getKey("abc123"), "t1", "t2", "t3");

            repository.forwardToUsers(nearbyUsers, new Post("a", currentUserLocation, LocalDateTime.now()));

            assertThat(jedis.llen(getKey("abc123")), equalTo(4L));
            assertThat(jedis.lrange(getKey("abc123"), 0, -1), hasItems("a", "t1", "t2", "t3"));
        }
    }

    @Test
    void testFanoutInCache_IndexOccupiedByNonList()
    {
        HybridPostRepository repository = new HybridPostRepository(template, jedisPool);
        List<UserLocation> nearbyUsers = new ArrayList<>();
        nearbyUsers.add(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        nearbyUsers.add(new UserLocation("xyz", new UserLocation.Position(22.507449, 88.32), 2100));

        UserLocation currentUserLocation = new UserLocation("test", new UserLocation.Position(22.72, 87.34),
                                                            1100);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(getKey("abc123"), "evil");

            repository.forwardToUsers(nearbyUsers, new Post("a", currentUserLocation, LocalDateTime.now()));

            assertTrue(jedis.exists(getKey("abc123")));
            assertThat(jedis.lpop(getKey("abc123")), equalTo("a"));
        }
    }

    private String getKey(String id)
    {
        return FEED_NAMESPACE + ":" + id;
    }
}