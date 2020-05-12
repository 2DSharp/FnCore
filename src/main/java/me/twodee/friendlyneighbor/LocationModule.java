package me.twodee.friendlyneighbor;

import com.google.inject.AbstractModule;
import me.twodee.friendlyneighbor.repository.LocationRepository;
import me.twodee.friendlyneighbor.repository.MongoLocationRepository;

public class LocationModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(LocationRepository.class).to(MongoLocationRepository.class);
    }
}
