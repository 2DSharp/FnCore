package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.Location;

public interface LocationRepository
{
    /**
     * Saves a user's location data
     * @param location
     * @return
     */
    Location save(Location location);


}
