package me.twodee.friendlyneighbor;

import com.mongodb.DBObject;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import me.twodee.friendlyneighbor.Entity.Location;
import org.hamcrest.CoreMatchers;
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
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    void test() throws Exception {

        template.indexOps(Location.class).ensureIndex(new GeospatialIndex("position") );

        template.save(new Location("International", new double[]{-73.9667, 40.738868}, 2100.0));
        template.save(new Location("Mumbai", new double[]{ 72.807543, 19.416575 }, 1100));
        template.save(new Location("Kolkata", new double[]{ 88.329317, 22.507449 }, 2100));
        template.save(new Location("Delhi", new double[]{77.2258241, 28.5837057}, 2100));

        Point location = new Point(88.414486, 22.623806 );
        Distance distance = new Distance(2100, Metrics.KILOMETERS);
        NearQuery query = NearQuery.near(location).maxDistance(distance);
        GeoNearOperation nearestPoints = Aggregation.geoNear(query, "dis");
        MatchOperation withinRadiusOfNearestPoints = Aggregation.match(
                Criteria.where("radius").gte(2100));

        Aggregation aggregation = Aggregation.newAggregation(nearestPoints, withinRadiusOfNearestPoints);
        AggregationResults<Location> res = template.aggregate(aggregation, "location", Location.class);

        assertThat(res.getMappedResults().size(), equalTo(2));
         res.getMappedResults().forEach(r -> assertThat(r.getId(), anyOf(equalTo("Kolkata"), equalTo("Delhi"))));
    }
}