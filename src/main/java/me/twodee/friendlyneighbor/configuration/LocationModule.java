package me.twodee.friendlyneighbor.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import me.twodee.friendlyneighbor.repository.HybridPostRepository;
import me.twodee.friendlyneighbor.repository.LocationRepository;
import me.twodee.friendlyneighbor.repository.MongoLocationRepository;
import me.twodee.friendlyneighbor.repository.PostRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.util.Properties;

public class LocationModule extends AbstractModule
{
    @Override
    protected void configure()

    {
        bind(LocationRepository.class).to(MongoLocationRepository.class);
        bind(PostRepository.class).to(HybridPostRepository.class);
    }

    @Provides
    MongoTemplate provideMongoTemplate() throws IOException
    {
        Properties prop = new Properties();
        prop.load(getClass().getClassLoader().getResourceAsStream("config.properties"));

        return new MongoTemplate(MongoClients.create(new ConnectionString(
                "mongodb://" + prop.getProperty("mongo.hostname") + ":" + prop.getProperty("mongo.port"))),
                                 prop.getProperty("mongo.database"));
    }
}
