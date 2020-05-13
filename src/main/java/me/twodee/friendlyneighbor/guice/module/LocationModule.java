package me.twodee.friendlyneighbor.guice.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.mongodb.client.MongoClients;
import me.twodee.friendlyneighbor.repository.LocationRepository;
import me.twodee.friendlyneighbor.repository.MongoLocationRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public class LocationModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(LocationRepository.class).to(MongoLocationRepository.class);
    }

    @Provides
    MongoTemplate provideMongoTemplate()
    {
       return new MongoTemplate(MongoClients.create(), "test_friendly_neighbor");
    }
}
