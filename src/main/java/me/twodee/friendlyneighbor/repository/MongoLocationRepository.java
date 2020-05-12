package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.Location;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;

import javax.inject.Inject;

public class MongoLocationRepository implements LocationRepository
{
    private final MongoTemplate template;

    @Inject
    public MongoLocationRepository(MongoTemplate template)
    {
        this.template = template;
        initTemplate();
    }

    private void initTemplate()
    {
        template.indexOps(Location.class).ensureIndex(new GeospatialIndex("position"));
    }

    @Override
    public Location save(Location location)
    {
        return template.save(location);
    }
}
