package me.twodee.friendlyneighbor.service;

import lombok.extern.java.Log;
import me.twodee.friendlyneighbor.dto.Notification;
import me.twodee.friendlyneighbor.dto.ResultObject;
import me.twodee.friendlyneighbor.dto.UserLocationsResult;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.exception.InvalidUser;
import me.twodee.friendlyneighbor.repository.LocationRepository;

import javax.inject.Inject;
import java.util.Arrays;

@Log
public class Discovery
{
    private final LocationRepository repository;

    @Inject
    public Discovery(LocationRepository repository)
    {
        this.repository = repository;
    }

    public ResultObject saveUserLocation(UserLocation location)
    {
        try {
            repository.save(location);
            return new ResultObject();
        } catch (Throwable e) {
            return somethingWentWrong(e);
        }
    }

    public ResultObject deleteUserLocation(String id)
    {
        try {
            repository.deleteById(id);
            return new ResultObject();
        } catch (Throwable e) {
            return somethingWentWrong(e);
        }

    }

    public UserLocationsResult lookupNearbyUsersByUserId(String requestingUid)
    {
        try {
            return new UserLocationsResult(repository.getUsersNearBy(requestingUid));
        } catch (InvalidUser e) {
            return buildErrorDTO(e);
        }
    }

    public UserLocationsResult lookupNearbyUsersByLocation(UserLocation location)
    {
        try {
            return new UserLocationsResult(repository.getUsersNearBy(location));
        } catch (InvalidUser e) {
            return buildErrorDTO(e);
        }
    }

    private ResultObject somethingWentWrong(Throwable e)
    {
        log.severe(Arrays.toString(e.getStackTrace()));
        return new ResultObject("internal", ResultObject.SOMETHING_WENT_WRONG);
    }

    private UserLocationsResult buildErrorDTO(Throwable e)
    {
        Notification note = new Notification();
        if (e instanceof InvalidUser) {
            note.addError("userId", "The supplied User ID doesn't exist");
        }
        return new UserLocationsResult(note);
    }

}
