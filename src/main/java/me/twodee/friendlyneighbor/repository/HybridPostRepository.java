package me.twodee.friendlyneighbor.repository;

import lombok.extern.slf4j.Slf4j;
import me.twodee.friendlyneighbor.component.FnCoreConfig;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.twodee.friendlyneighbor.component.Util.haversine;

/**
 * HybridPostRepository uses Redis for caching posts along with Mongo for permanent storage
 */
@Slf4j
public class HybridPostRepository implements PostRepository
{
    private final MongoTemplate mongoTemplate;
    private final JedisPool jedisPool;
    private static final String DEFAULT_NAMESPACE = "FN_CORE";
    private static final String FEED_KEYSPACE = "FEED";
    private String feedNamespace = DEFAULT_NAMESPACE + "." + FEED_KEYSPACE;
    public static final long CACHE_EXPIRY_DEFAULT_DAYS = 10;
    private long expiryInDays = CACHE_EXPIRY_DEFAULT_DAYS;

    /**
     * @param mongoTemplate
     * @param jedisPool
     * @deprecated Use a config file instead, kept for testing
     */
    public HybridPostRepository(MongoTemplate mongoTemplate, JedisPool jedisPool)
    {
        this.mongoTemplate = mongoTemplate;
        this.jedisPool = jedisPool;
        initProperties();
    }

    @Inject
    public HybridPostRepository(MongoTemplate mongoTemplate, JedisPool jedisPool, FnCoreConfig config) {
        this.mongoTemplate = mongoTemplate;
        this.jedisPool = jedisPool;
        expiryInDays = config.getFeedCacheExpiry();
        feedNamespace = config.getRedisKeyspace() + "." + FEED_KEYSPACE;
        //initIndexes();
    }

    private void initIndexes() {
        TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("title", 2F)
                .build();
        mongoTemplate.indexOps(Post.class)
                .ensureIndex(textIndex);
    }

    @Override
    public Post save(Post post) {
        return mongoTemplate.save(post);
    }

    @Override
    public void forwardToUsers(List<UserLocation> userLocations, Post post) {
        userLocations.parallelStream()
                .forEach(location -> fanout(location, post));
    }

    @Override
    public void deleteById(String id)
    {
        mongoTemplate.remove(Query.query(Criteria.where("id").is(id)), Post.class);
    }

    /**
     *
     */
    private void initProperties()
    {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            expiryInDays = Long.parseLong(
                    properties.getProperty("feed.expiry", String.valueOf(CACHE_EXPIRY_DEFAULT_DAYS)));
        } catch (IOException e) {
            expiryInDays = CACHE_EXPIRY_DEFAULT_DAYS;
            log.error(e.getMessage(), e);
        }
    }

    private String serializeForRedis(String postId)
    {
        return postId;
    }

    private String deserializeIdFromRedis(String serializedVal)
    {
        return serializedVal.split(":")[0];
    }


    /**
     * Fan-out to all users in the vicinity. We can optimize by using LPUSHX and hydrating on pull later
     * If we happen to have a lot of users. For a few couple hundred, create their own timelines.
     * Once we start running out of RAM, start evicting old feeds.
     *
     * @param otherUserLocation
     * @param post
     */
    private void fanout(UserLocation otherUserLocation, Post post)
    {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(otherUserLocation.getId());
            String toStore = serializeForRedis(post.getId());
            try {
                // Create their feed, since we expect smaller numbers
                // Expires after 30 days
                if (!jedis.exists(key)) {
                    putIntoList(jedis, key, toStore);
                }
                else {
                    // Append to the already existing list
                    jedis.lpushx(key, toStore);
                }
            } catch (JedisDataException e) {
                // Someone occupied the list
                // Claim it back
                log.warn("Invalid type value has been occupying keyspace " + key);
                jedis.del(key);
                putIntoList(jedis, key, toStore);
            }
        }
    }

    private void putIntoList(Jedis jedis, String key, String value)
    {
        jedis.lpush(key, value);
        jedis.expire(key, (int) TimeUnit.DAYS.toSeconds(expiryInDays));
    }

    private String getKey(String id)
    {
        return feedNamespace + ":" + id;
    }

    @Override
    public List<Post> findAllForUser(UserLocation userLocation, List<UserLocation> nearbyUsers)
    {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(userLocation.getId());

            if (jedis.exists(key)) {

                try {
                    // He's fresh, reset expiry
                    jedis.expire(key, (int) TimeUnit.DAYS.toSeconds(expiryInDays));
                    // Return the entire list, for now
                    return processPostDistances(fetchPostsByPostIds(jedis.lrange(key, 0, -1)), userLocation);
                } catch (JedisDataException e) {
                    log.warn("Invalid type value has been occupying keyspace " + key);
                    jedis.del(key);
                    return processPostDistances(fetchAndRehydrate(key, nearbyUsers, jedis), userLocation);
                }
            }
            else {
                return processPostDistances(fetchAndRehydrate(key, nearbyUsers, jedis), userLocation);
            }
        }
    }

    @Override
    public List<Post> findAllForUser(UserLocation currentUserLocation) {
        GeoResults<Post> geoResults = mongoTemplate.query(Post.class)
                .as(Post.class)
                .near(createNearQuery(currentUserLocation.getPosition(), currentUserLocation.getRadius()))
                .all();
        return geoResults.getContent().stream()
                .filter(result -> filterIneligibleResults(result, currentUserLocation.getId()))
                .map(this::setDistanceInGeoResult)
                .collect(Collectors.toList());
    }

    private boolean filterIneligibleResults(GeoResult<Post> result, String exclude) {
        return (result.getDistance().in(Metrics.KILOMETERS).getValue() <= result.getContent().getLocation().getRadius())
                && (!result.getContent().getLocation().getId().equals(exclude));
    }

    private Post setDistanceInGeoResult(GeoResult<Post> locationGeoResult) {
        Post post = locationGeoResult.getContent();
        post.getLocation().setDistance(locationGeoResult.getDistance().in(Metrics.KILOMETERS).getValue());
        return post;
    }

    private NearQuery createNearQuery(UserLocation.Position position, double radius) {
        Point location = new Point(position.getLongitude(), position.getLatitude());
        Distance distance = new Distance(radius, Metrics.KILOMETERS);

        return NearQuery.near(location).maxDistance(distance);

    }

    @Override
    public List<Post> fetchMatchingNearbyPosts(UserLocation currentUserLocation,
                                               List<UserLocation> nearbyUsers,
                                               Post post) {
        List<String> ids = nearbyUsers.stream()
                .map(UserLocation::getId)
                .collect(Collectors.toList());
        return fetchPostsByLocationIdsAndCloseness(ids, post.getType(), post.getTitle());
    }

    private List<Post> processPostDistances(List<Post> posts, UserLocation currentUserLocation) {
        posts.forEach(post -> updateDistanceAndPosition(currentUserLocation, post));
        return posts;
    }

    private List<Post> fetchAndRehydrate(String key, List<UserLocation> nearbyUsers, Jedis jedis) {
        List<String> ids = nearbyUsers.stream()
                .map(UserLocation::getId)
                .collect(Collectors.toList());
        List<Post> results = fetchPostsByLocationIds(ids);
        // Hydrate the feed of the user
        // Attach at the end of the array, thus preserving order
        results.forEach(post -> jedis.rpush(key, post.getId()));
        return results;
    }

    private void updateDistanceAndPosition(UserLocation currentUserLocation, Post post)
    {
        UserLocation.Position postPosition = post.getLocation().getPosition();
        UserLocation.Position currentUserPosition = currentUserLocation.getPosition();
        post.getLocation().setDistance(haversine(postPosition.getLatitude(),
                                                 postPosition.getLongitude(),
                                                 currentUserPosition.getLatitude(),
                                                 currentUserPosition.getLongitude()));
        post.getLocation().setPosition(null);
    }


    private List<Post> fetchPostsByPostIds(List<String> postIdsAndDistance)
    {
        List<String> postIds = postIdsAndDistance.stream()
                .map(this::deserializeIdFromRedis)
                .collect(Collectors.toList());

        Query query = Query.query(Criteria.where("id").in(postIds)).with(Sort.by(Sort.Direction.DESC, "time"));

        return mongoTemplate.find(query, Post.class);
    }

    private List<Post> fetchPostsByLocationIds(List<String> locations) {
        Query query = Query.query(Criteria.where("location.id").in(locations)).with(
                Sort.by(Sort.Direction.DESC, "time"));

        return mongoTemplate.find(query, Post.class);
    }

    private List<Post> fetchPostsByLocationIdsAndCloseness(List<String> locations, Post.PostType type, String text) {
        Term term = new Term(text);

        TextQuery query = TextQuery.queryText(new TextCriteria().matching(term).diacriticSensitive(false)).includeScore(
                "score").sortByScore();
        query.addCriteria(Criteria.where("location.id").in(locations).and("type").nin(type)).with(
                Sort.by(Sort.Direction.DESC, "time"));
        System.out.println(query.toString());
        return mongoTemplate.find(query, Post.class);
    }
}
