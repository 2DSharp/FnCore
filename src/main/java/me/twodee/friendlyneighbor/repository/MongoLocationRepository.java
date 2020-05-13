package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.UserLocation;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GeoNearOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MongoLocationRepository implements LocationRepository
{
    private final MongoTemplate template;

    @Inject
    public MongoLocationRepository(MongoTemplate template)
    {
        this.template = template;
        initTemplate();
    }

    private void initTemplate()
    {
        template.indexOps(UserLocation.class).ensureIndex(new GeospatialIndex("position"));
    }

    @Override
    public UserLocation save(UserLocation userLocation)
    {
        return template.save(userLocation);
    }

    @Override
    public List<UserLocation> getUsersNearBy(String userId)
    {
        UserLocation userLocation = template.findById(userId, UserLocation.class);

        if (userLocation != null) {
            return getUsersInGivenLocation(userLocation.getPosition(), userLocation.getRadius(), userId);
        }
        return Collections.emptyList();
    }

    private List<UserLocation> getUsersInGivenLocation(UserLocation.Position position, double radius, String exclude)
    {
        GeoResults<UserLocation> geoResults = template.query(UserLocation.class)
            .as(UserLocation.class)
            .near(createNearQuery(position, radius))
            .all();

        return geoResults.getContent().stream()
                .filter(result -> filterIneligibleResults(result, exclude))
                .map(this::setDistanceInGeoResult)
                .collect(Collectors.toList());
    }

    private boolean filterIneligibleResults(GeoResult<UserLocation> result, String exclude)
    {
        return (result.getDistance().in(Metrics.KILOMETERS).getValue() <= result.getContent().getRadius())
                && (!result.getContent().getId().equals(exclude));
    }

    private UserLocation setDistanceInGeoResult(GeoResult<UserLocation> locationGeoResult)
    {
        UserLocation location = locationGeoResult.getContent();
        location.setDis(locationGeoResult.getDistance().in(Metrics.KILOMETERS).getValue());
        return location;
    }


    private NearQuery createNearQuery(UserLocation.Position position, double radius)
    {
        Point location = new Point(position.getLongitude(), position.getLatitude());
        Distance distance = new Distance(radius, Metrics.KILOMETERS);

        return NearQuery.near(location).maxDistance(distance);
    }


}
