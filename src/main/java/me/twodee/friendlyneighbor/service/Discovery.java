package me.twodee.friendlyneighbor.service;

import me.twodee.friendlyneighbor.entity.Location;
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
        Location location = new Location(request.getUserId(),
                                         new double[]{request.getLocation().getLongitude(),
                                                 request.getLocation().getLatitude()},
                                         request.getRadius());
        repository.save(location);
    }
}
