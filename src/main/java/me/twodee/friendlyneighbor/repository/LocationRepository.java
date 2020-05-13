package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.UserLocation;

import java.util.List;

public interface LocationRepository
{
    /**
     * Saves a user's userLocation data
     * @param userLocation User data with location/radius information in KM
     * @return Returns the persisted user location
     */
    UserLocation save(UserLocation userLocation);

    /**
     * Look up users who are near a given user who is already registered
     * @param userId The user looking up nearby users
     * @return A list of users with their distance
     */
    List<UserLocation> getUsersNearBy(String userId);

}
