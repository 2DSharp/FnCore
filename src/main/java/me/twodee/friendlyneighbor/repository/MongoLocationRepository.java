package me.twodee.friendlyneighbor.repository;

import lombok.extern.slf4j.Slf4j;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.exception.DbFailure;
import me.twodee.friendlyneighbor.exception.InvalidUser;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MongoLocationRepository implements LocationRepository
{
    private final MongoTemplate template;
    private static final int MONGO_INDEX_NOT_FOUND = 27;

    @Inject
    public MongoLocationRepository(MongoTemplate template)
    {
        this.template = template;
        initTemplate();
    }

    private void initTemplate()
    {
        template.indexOps(UserLocation.class).ensureIndex(
                new GeospatialIndex("position").typed(GeoSpatialIndexType.GEO_2DSPHERE));
    }

    @Override
    public UserLocation save(UserLocation userLocation)
    {
        return template.save(userLocation);
    }

    @Override
    public List<UserLocation> getUsersNearBy(String userId) throws InvalidUser, DbFailure
    {
        UserLocation userLocation = template.findById(userId, UserLocation.class);

        if (userLocation != null) {
            return getUsersInGivenLocation(userLocation.getPosition(), userLocation.getRadius(), userId);
        }
        throw new InvalidUser("The user id supplied doesn't exist");
    }

    @Override
    public List<UserLocation> getUsersNearBy(UserLocation userLocation) throws InvalidUser, DbFailure
    {
        if (template.findById(userLocation.getId(), UserLocation.class) == null) {
            throw new InvalidUser("The user id supplied doesn't exist");
        }
        return getUsersInGivenLocation(userLocation.getPosition(), userLocation.getRadius(), userLocation.getId());
    }

    @Override
    public UserLocation findById(String id)
    {
        return template.findById(id, UserLocation.class);
    }

    @Override
    public void deleteById(String id)
    {
        template.remove(Query.query(Criteria.where("id").is(id)), UserLocation.class);
    }

    private List<UserLocation> getUsersInGivenLocation(UserLocation.Position position, double radius, String exclude) throws DbFailure
    {
        try {
            GeoResults<UserLocation> geoResults = template.query(UserLocation.class)
                    .as(UserLocation.class)
                    .near(createNearQuery(position, radius))
                    .all();

            return geoResults.getContent().stream()
                    .filter(result -> filterIneligibleResults(result, exclude))
                    .map(this::setDistanceInGeoResult)
                    .collect(Collectors.toList());
        } catch (UncategorizedMongoDbException e) {

            log.error("Severe DB failure, did you drop a collection? ", e);
            throw new DbFailure(e);
        }
    }

    private boolean filterIneligibleResults(GeoResult<UserLocation> result, String exclude)
    {
        return (result.getDistance().in(Metrics.KILOMETERS).getValue() <= result.getContent().getRadius())
                && (!result.getContent().getId().equals(exclude));
    }

    private UserLocation setDistanceInGeoResult(GeoResult<UserLocation> locationGeoResult)
    {
        UserLocation location = locationGeoResult.getContent();
        location.setDistance(locationGeoResult.getDistance().in(Metrics.KILOMETERS).getValue());
        return location;
    }

    private NearQuery createNearQuery(UserLocation.Position position, double radius) throws DbFailure
    {
        Point location = new Point(position.getLongitude(), position.getLatitude());
        Distance distance = new Distance(radius, Metrics.KILOMETERS);

        return NearQuery.near(location).maxDistance(distance);

    }
}
