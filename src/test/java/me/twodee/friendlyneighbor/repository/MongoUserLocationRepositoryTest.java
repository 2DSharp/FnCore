package me.twodee.friendlyneighbor.repository;

import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import me.twodee.friendlyneighbor.entity.UserLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class MongoUserLocationRepositoryTest
{
    private MongodExecutable mongodExecutable;
    private MongoTemplate template;

    @AfterEach
    void clean()
    {
        mongodExecutable.stop();
    }

    @BeforeEach
    void setup() throws Exception
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

    @Test
    void testSuccessfulSave()
    {
        LocationRepository repository = new MongoLocationRepository(template);

        UserLocation result = repository.save(
                new UserLocation("Mumbai", new UserLocation.Position(19.416575, 72.807543), 1100));

        assertThat(result.getId(), equalTo("Mumbai"));
        assertThat(template.findAll(UserLocation.class).size(), equalTo(1));
        assertThat(template.findById("Mumbai", UserLocation.class).getRadius(), equalTo(1100.0));
    }

    @Test
    void testLookupByIdDoesntContainSameLocation()
    {
        LocationRepository repository = new MongoLocationRepository(template);
        template.save(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        template.save(new UserLocation("Kolkata", new UserLocation.Position(22.507449, 88.329317), 2100));

        List<UserLocation> users = repository.getUsersNearBy("abc123");

        assertThat(users).extracting("id").doesNotContain("abc123");
    }

    @Test
    void testLookupByIdDoesntContainLocationOutOfRange()
    {
        LocationRepository repository = new MongoLocationRepository(template);
        template.save(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        template.save(new UserLocation("Outside", new UserLocation.Position(10.5837057, 70.2258241), 100));

        List<UserLocation> users = repository.getUsersNearBy("abc123");

        assertThat(users).extracting("id").doesNotContain("Outside");
    }

    @Test
    void testLookupByIdDoesntContainLocationInRangeWithSmallRadius()
    {
        LocationRepository repository = new MongoLocationRepository(template);
        template.save(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        template.save(new UserLocation("Delhi", new UserLocation.Position(28.2258241, 77.5837057), 1200));

        List<UserLocation> users = repository.getUsersNearBy("abc123");
        users.forEach(r -> System.out.println(r.getDis()));
        assertThat(users).extracting("id").doesNotContain("Delhi");
    }

    @Test
    void testLookupByIdContainsLocationWithSmallButEnoughRange()
    {
        LocationRepository repository = new MongoLocationRepository(template);
        template.save(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        template.save(new UserLocation("Kolkata", new UserLocation.Position(22.507449, 88.329317), 100));

        List<UserLocation> users = repository.getUsersNearBy("abc123");
        users.forEach(r -> System.out.println(r.getDis()));
        assertThat(users).extracting("id").containsExactly("Kolkata");
    }

    @Test
    void testSuccessfulLookupById()
    {
        LocationRepository repository = new MongoLocationRepository(template);
        template.save(new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100));
        template.save(new UserLocation("Kolkata", new UserLocation.Position(22.507449, 88.329317), 2100));
        template.save(new UserLocation("Delhi", new UserLocation.Position(28.2258241, 77.5837057), 2100));
        template.save(new UserLocation("Outside", new UserLocation.Position(10.5837057, 70.2258241), 100));

        List<UserLocation> users = repository.getUsersNearBy("abc123");

        assertThat(users).extracting("id").containsOnly("Kolkata", "Delhi");
    }
}