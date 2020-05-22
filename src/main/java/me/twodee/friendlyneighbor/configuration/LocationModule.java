package me.twodee.friendlyneighbor.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import me.twodee.friendlyneighbor.component.FnCoreConfig;
import me.twodee.friendlyneighbor.repository.HybridPostRepository;
import me.twodee.friendlyneighbor.repository.LocationRepository;
import me.twodee.friendlyneighbor.repository.MongoLocationRepository;
import me.twodee.friendlyneighbor.repository.PostRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.inject.Singleton;

@Slf4j
public class LocationModule extends AbstractModule
{
    private final FnCoreConfig config;

    public LocationModule(FnCoreConfig config)
    {
        this.config = config;
    }

    @Override
    protected void configure()
    {
        bind(LocationRepository.class).to(MongoLocationRepository.class);
        bind(PostRepository.class).to(HybridPostRepository.class);
    }

    @Provides
    @Singleton
    MongoTemplate provideMongoTemplate()
    {
        try {
            MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create(new ConnectionString(
                    config.getMongoConnectionString()
            )), config.getMongoDatabase());
            log.info("Connected to mongo server.");
            return mongoTemplate;
        } catch (Exception e) {
            log.error("Mongo connection failed!", e);
            return null;
        }
    }

    // TODO: Add additional Jedis config, warm up at instantiation. Create a separate provider.
    // https://partners-intl.aliyun.com/help/doc-detail/98726.htm
    @Singleton
    @Provides
    JedisPool provideJedisPool()
    {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        return new JedisPool(poolConfig, config.getRedisHostName(), config.getRedisPort());
    }

    @Provides
    HybridPostRepository provideHybridPostRepository()
    {
        return new HybridPostRepository(provideMongoTemplate(), provideJedisPool(), config);
    }

}
