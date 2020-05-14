package me.twodee.friendlyneighbor.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import me.twodee.friendlyneighbor.Main;
import me.twodee.friendlyneighbor.repository.LocationRepository;
import me.twodee.friendlyneighbor.repository.MongoLocationRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.util.Properties;

public class LocationModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(LocationRepository.class).to(MongoLocationRepository.class);
    }

    @Provides
    MongoTemplate provideMongoTemplate() throws IOException
    {
        Properties properties = new Properties();
        properties.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));

        return new MongoTemplate(MongoClients.create(new ConnectionString(
                "mongodb://" + properties.getProperty("mongo.hostname") + ":" + properties.getProperty("mongo.port"))),
                                 properties.getProperty("mongo.database"));
    }
}
