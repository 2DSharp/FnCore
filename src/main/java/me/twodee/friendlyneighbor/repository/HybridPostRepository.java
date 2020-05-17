package me.twodee.friendlyneighbor.repository;

import lombok.extern.slf4j.Slf4j;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * HybridPostRepository uses Redis for caching posts along with Mongo for permanent storage
 */
@Slf4j
public class HybridPostRepository implements PostRepository
{
    private final MongoTemplate mongoTemplate;
    private final JedisPool jedisPool;
    private static final String FEED_NAMESPACE = "FN_CORE.FEED";

    @Inject
    public HybridPostRepository(MongoTemplate mongoTemplate, JedisPool jedisPool)
    {
        this.mongoTemplate = mongoTemplate;
        this.jedisPool = jedisPool;
    }

    @Override
    public Post save(Post post)
    {
        return mongoTemplate.save(post);
    }

    @Override
    public void forwardToUsers(List<UserLocation> userLocations, Post post)
    {
        userLocations.parallelStream()
                .forEach(location -> fanout(location, post));
    }

    /**
     * Fan-out to all users in the vicinity. We can optimize by using LPUSHX and hydrating on pull later
     * If we happen to have a lot of users. For a few couple hundred, create their own timelines.
     * Once we start running out of RAM, start evicting old feeds.
     *
     * @param location
     * @param post
     */
    private void fanout(UserLocation location, Post post)
    {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(location.getId());
            try {
                // Create their feed, since we expect smaller numbers
                // Expires after 30 days
                if (!jedis.exists(key)) {
                    putIntoList(jedis, key, post.getId());
                }
                else {
                    // Append to the already existing list
                    jedis.lpushx(key, post.getId());
                }
            } catch (JedisDataException e) {
                // Someone occupied the list
                // Claim it back
                log.warn("Invalid type value has been occupying keyspace " + key);
                jedis.del(key);
                putIntoList(jedis, key, post.getId());
            }
        }
    }

    private void putIntoList(Jedis jedis, String key, String value)
    {
        jedis.lpush(key, value);
        jedis.expire(key, (int) TimeUnit.DAYS.toSeconds(30));
    }

    private String getKey(String id)
    {
        return FEED_NAMESPACE + ":" + id;
    }

    @Override
    public List<Post> findAllForUser(String userId, List<UserLocation> nearbyUsers)
    {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(userId);

            if (jedis.exists(key)) {
                try {
                    // He's fresh, reset expiry
                    jedis.expire(key, (int) TimeUnit.DAYS.toSeconds(30));
                    // Return the entire list, for now
                    return fetchPosts(jedis.lrange(key, 0, -1));
                } catch (JedisDataException e) {
                    log.warn("Invalid type value has been occupying keyspace " + key);
                    jedis.del(key);
                    return fetchAndRehydrate(key, nearbyUsers, jedis);
                }
            }
            else {
                return fetchAndRehydrate(key, nearbyUsers, jedis);
            }
        }
    }

    private List<Post> fetchAndRehydrate(String key, List<UserLocation> nearbyUsers, Jedis jedis)
    {
        List<String> ids = nearbyUsers.stream()
                .map(UserLocation::getId)
                .collect(Collectors.toList());
        List<Post> results = pullPosts(ids);
        // Hydrate the feed of the user
        // Attach at the end of the array, thus preserving order
        results.forEach(post -> jedis.rpush(key, post.getId()));
        return results;
    }

    private List<Post> fetchPosts(List<String> postIds)
    {
        Query query = Query.query(Criteria.where("id").in(postIds)).with(Sort.by(Sort.Direction.DESC, "time"));
        query.fields().exclude("location.position");
        return mongoTemplate.find(query, Post.class);
    }

    private List<Post> pullPosts(List<String> locations)
    {
        Query query = Query.query(Criteria.where("location.id").in(locations)).with(
                Sort.by(Sort.Direction.DESC, "time"));
        query.fields().exclude("location.position");
        return mongoTemplate.find(query, Post.class);
    }
}
