package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.Location;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.inject.Inject;

public class MongoLocationRepository implements LocationRepository
{
    private final MongoTemplate template;

    @Inject
    public MongoLocationRepository(MongoTemplate template)
    {
        this.template = template;
    }

    @Override
    public Location save(Location location)
    {
        return template.save(location);
    }
}
