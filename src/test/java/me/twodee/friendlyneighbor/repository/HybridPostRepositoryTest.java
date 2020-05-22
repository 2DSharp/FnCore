package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.component.FnCoreConfig;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

class HybridPostRepositoryTest
{
    @Mock
    MongoTemplate template;
    @Mock
    JedisPool pool;

    @Mock
    Jedis jedis;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void successfulSave()
    {
        Post post = new Post();
        when(template.save(post)).thenReturn(post);
        FnCoreConfig config = FnCoreConfig.builder()
                .redisKeyspace("FNCORE")
                .feedCacheExpiry(20)
                .build();

        HybridPostRepository repository = new HybridPostRepository(template, pool, config);
        Post result = repository.save(post);

        assertThat(result, equalTo(post));
    }

    @Test
    void successfulForwardToUsersOutOfCache()
    {
        FnCoreConfig config = FnCoreConfig.builder()
                .redisKeyspace("FNCORE")
                .feedCacheExpiry(20)
                .build();
        Post post = new Post();
        post.setId("a");
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.exists(anyString())).thenReturn(false);

        List<UserLocation> locationList = new ArrayList<>();

        locationList.add(new UserLocation());
        locationList.add(new UserLocation());

        HybridPostRepository repository = new HybridPostRepository(template, pool, config);
        repository.forwardToUsers(locationList, post);

        verify(jedis, times(2)).lpush(anyString(), anyString());
        verify(jedis, times(2)).expire(anyString(), anyInt());
    }

    @Test
    void successfulForwardToUsersInCache()
    {
        FnCoreConfig config = FnCoreConfig.builder()
                .redisKeyspace("FNCORE")
                .feedCacheExpiry(20)
                .build();
        Post post = new Post();
        post.setId("a");
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.exists(anyString())).thenReturn(true);

        List<UserLocation> locationList = new ArrayList<>();

        locationList.add(new UserLocation());
        locationList.add(new UserLocation());

        HybridPostRepository repository = new HybridPostRepository(template, pool, config);
        repository.forwardToUsers(locationList, post);

        verify(jedis, times(2)).lpushx(anyString(), anyString());
        verify(jedis, times(0)).expire(anyString(), anyInt());
    }

    @Test
    void successfulForwardToUsersInAndOutOfCache()
    {
        FnCoreConfig config = FnCoreConfig.builder()
                .redisKeyspace("FN_CORE")
                .feedCacheExpiry(20)
                .build();
        Post post = new Post();
        post.setId("a");
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.exists("FN_CORE.FEED:a")).thenReturn(true);
        when(jedis.exists("FN_CORE.FEED:b")).thenReturn(false);

        List<UserLocation> locationList = new ArrayList<>();

        locationList.add(new UserLocation("a", new UserLocation.Position(0, 0), 0));
        locationList.add(new UserLocation("b", new UserLocation.Position(0, 0), 0));

        HybridPostRepository repository = new HybridPostRepository(template, pool, config);
        repository.forwardToUsers(locationList, post);

        // not in cache
        verify(jedis, times(1)).lpush("FN_CORE.FEED:b", "a");
        verify(jedis, times(1)).expire(anyString(), anyInt());

        // in cache
        verify(jedis, times(1)).lpushx("FN_CORE.FEED:a", "a");
    }

    @Test
    void findAllPostsForUserInCache()
    {
        FnCoreConfig config = FnCoreConfig.builder()
                .redisKeyspace("FNCORE")
                .feedCacheExpiry(20)
                .build();
        HybridPostRepository repository = new HybridPostRepository(template, pool, config);
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.exists(anyString())).thenReturn(true);
        List<String> postIds = new ArrayList<>();
        postIds.add("");
        postIds.add("");
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("a", new UserLocation("x", new UserLocation.Position(0, 0), 0), LocalDateTime.now()));
        posts.add(new Post("b", new UserLocation("x", new UserLocation.Position(0, 0), 0), LocalDateTime.now()));

        when(jedis.lrange(anyString(), anyLong(), anyLong())).thenReturn(postIds);
        when(template.find(any(), any(Class.class))).thenReturn(posts);


        List<UserLocation> locationList = new ArrayList<>();

        locationList.add(new UserLocation("x", new UserLocation.Position(0, 0), 0));
        locationList.add(new UserLocation("y", new UserLocation.Position(0, 0), 0));

        List<Post> result = repository.findAllForUser(
                new UserLocation("test", new UserLocation.Position(22.507449, 88.34), 2100), locationList);

        Assertions.assertThat(result).extracting("id").contains("a", "b");
    }

    @Test
    void findAllPostsForUserNotInCache()
    {
        FnCoreConfig config = FnCoreConfig.builder()
                .redisKeyspace("FNCORE")
                .feedCacheExpiry(20)
                .build();
        HybridPostRepository repository = new HybridPostRepository(template, pool, config);
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.exists(anyString())).thenReturn(false);
        List<String> postIds = new ArrayList<>();
        postIds.add("");
        postIds.add("");
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("a", new UserLocation("x", new UserLocation.Position(0, 0), 0), LocalDateTime.now()));
        posts.add(new Post("b", new UserLocation("x", new UserLocation.Position(0, 0), 0), LocalDateTime.now()));

        when(template.find(any(), any(Class.class))).thenReturn(posts);


        List<UserLocation> locationList = new ArrayList<>();

        locationList.add(new UserLocation("x", new UserLocation.Position(0, 0), 0));
        locationList.add(new UserLocation("y", new UserLocation.Position(0, 0), 0));

        List<Post> result = repository.findAllForUser(
                new UserLocation("test", new UserLocation.Position(22.507449, 88.34), 2100), locationList);

        Assertions.assertThat(result).extracting("id").contains("a", "b");
    }

    @Test
    void fetchAlsoRetrievesDistance()
    {
        FnCoreConfig config = FnCoreConfig.builder()
                .redisKeyspace("FNCORE")
                .feedCacheExpiry(20)
                .build();
        HybridPostRepository repository = new HybridPostRepository(template, pool, config);
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.exists(anyString())).thenReturn(false);
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("a", new UserLocation("x", new UserLocation.Position(0, 0), 0), LocalDateTime.now()));

        when(template.find(any(), any(Class.class))).thenReturn(posts);

        List<UserLocation> locationList = new ArrayList<>();

        locationList.add(new UserLocation("x", new UserLocation.Position(0, 0), 0));

        List<Post> result = repository.findAllForUser(
                new UserLocation("test", new UserLocation.Position(22.507449, 88.34), 2100), locationList);

        Assertions.assertThat(result).extracting("location").extracting("distance").isNotNull();
    }
}