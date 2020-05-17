package me.twodee.friendlyneighbor;

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
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GeoNearOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


class GeoSearchTest
{
    private MongodExecutable mongodExecutable;
    private MongoTemplate template;

    @AfterEach
    void clean() {
        mongodExecutable.stop();
    }

    @BeforeEach
    void setup() throws Exception {
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
    void testGeoSearchBehavior()
    {
        template.indexOps(UserLocation.class).ensureIndex(new GeospatialIndex("position"));

        System.out.println(
                template.save(new UserLocation("Outside", new UserLocation.Position(10.5837057, 70.2258241), 100)));
        template.save(new UserLocation("Mumbai", new UserLocation.Position(19.416575, 72.807543), 1100));
        template.save(new UserLocation("Kolkata", new UserLocation.Position(22.507449, 88.329317), 2100));
        template.save(new UserLocation("Delhi", new UserLocation.Position(28.2258241, 77.5837057), 2100));

        Point location = new Point(88.414486, 22.623806);
        Distance distance = new Distance(2100, Metrics.KILOMETERS);
        NearQuery query = NearQuery.near(location).maxDistance(distance);
        GeoNearOperation nearestPoints = Aggregation.geoNear(query, "distance");
        MatchOperation withinRadiusOfNearestPoints = Aggregation.match(
                Criteria.where("radius").gte(2100));

        Aggregation aggregation = Aggregation.newAggregation(nearestPoints, withinRadiusOfNearestPoints);
        AggregationResults<UserLocation> res = template.aggregate(aggregation, "location", UserLocation.class);

        assertThat(res.getMappedResults().size(), equalTo(2));
         res.getMappedResults().forEach(r -> assertThat(r.getId(), anyOf(equalTo("Kolkata"), equalTo("Delhi"))));
    }
}