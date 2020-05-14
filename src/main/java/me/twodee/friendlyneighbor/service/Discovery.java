package me.twodee.friendlyneighbor.service;

import me.twodee.friendlyneighbor.FnCoreGenerated;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.exception.InvalidUser;
import me.twodee.friendlyneighbor.repository.LocationRepository;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Discovery
{
    private final LocationRepository repository;

    @Inject
    public Discovery(LocationRepository repository)
    {
        this.repository = repository;
    }

    public FnCoreGenerated.RequestResult registerUser(FnCoreGenerated.RegistrationRequest request)
    {
        UserLocation userLocation = new UserLocation(request.getUserId(),
                                                     new UserLocation.Position(request.getLocation().getLatitude(),
                                                                               request.getLocation().getLongitude()),
                                                     request.getRadius());
        if (repository.save(userLocation).equals(userLocation)) {
            return FnCoreGenerated.RequestResult.newBuilder().setSuccess(true).build();
        }
        return FnCoreGenerated.RequestResult.newBuilder().setSuccess(false).build();
    }

    public FnCoreGenerated.NearbyUsersResult lookupNearbyUsersByLocation(FnCoreGenerated.SearchAreaRequest request)
    {
        try {
            UserLocation userLocation = new UserLocation(request.getUserId(),
                                                         new UserLocation.Position(request.getLocation().getLatitude(),
                                                                                   request.getLocation().getLongitude()),
                                                         request.getRadius());
            List<FnCoreGenerated.UserNearby> users = buildUserList(repository.getUsersNearBy(userLocation));
            return buildResult(users);

        } catch (InvalidUser e) {
            return buildFailedResult(e);
        }
    }

    public FnCoreGenerated.NearbyUsersResult lookupNearbyUsersByUserId(FnCoreGenerated.UserIdentifier request)
    {
        try {
            List<FnCoreGenerated.UserNearby> users = buildUserList(repository.getUsersNearBy(request.getUserId()));
            return buildResult(users);

        } catch (InvalidUser e) {
            return FnCoreGenerated.NearbyUsersResult
                    .newBuilder()
                    .setMetaResult(FnCoreGenerated.RequestResult.newBuilder()
                                           .setSuccess(false)
                                           .putAllErrors(getErrors(e))
                                           .build()).build();
        }
    }

    private FnCoreGenerated.NearbyUsersResult buildFailedResult(Throwable e)
    {
        return FnCoreGenerated.NearbyUsersResult
                .newBuilder()
                .setMetaResult(FnCoreGenerated.RequestResult.newBuilder()
                                       .setSuccess(false)
                                       .putAllErrors(getErrors(e))
                                       .build()).build();
    }

    Map<String, String> getErrors(Throwable e)
    {
        Map<String, String> errors = new HashMap<>();
        if (e instanceof InvalidUser) {
            errors.put("userId", "The supplied User ID doesn't exist");
        }
        return errors;
    }

    private FnCoreGenerated.UserNearby createDtoUser(UserLocation userLocation)
    {
        return FnCoreGenerated.UserNearby.newBuilder()
                .setDistance(userLocation.getDis().doubleValue())
                .setUserId(userLocation.getId())
                .build();
    }

    private FnCoreGenerated.NearbyUsersResult buildResult(List<FnCoreGenerated.UserNearby> results)
    {
        return FnCoreGenerated.NearbyUsersResult.newBuilder()
                .setMetaResult(FnCoreGenerated.RequestResult.newBuilder().setSuccess(true).build())
                .addAllUser(results)
                .build();
    }

    private List<FnCoreGenerated.UserNearby> buildUserList(List<UserLocation> users)
    {
        return users.stream()
                .map(this::createDtoUser)
                .collect(Collectors.toList());
    }

    public FnCoreGenerated.RequestResult deleteUser(FnCoreGenerated.UserIdentifier request)
    {
        repository.deleteById(request.getUserId());
        return FnCoreGenerated.RequestResult.newBuilder().setSuccess(true).build();
    }
}
