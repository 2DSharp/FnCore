package me.twodee.friendlyneighbor.service;

import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.FnCoreGenerated;
import me.twodee.friendlyneighbor.repository.LocationRepository;

import javax.inject.Inject;

public class Discovery
{
    public final LocationRepository repository;

    @Inject
    public Discovery(LocationRepository repository)
    {
        this.repository = repository;
    }

    public void registerUser(FnCoreGenerated.RegistrationRequest request)
    {
        UserLocation userLocation = new UserLocation(request.getUserId(),
                                                     new UserLocation.Position(request.getLocation().getLatitude(),
                                                                               request.getLocation().getLongitude()),
                                                     request.getRadius());
        repository.save(userLocation);
    }
}
