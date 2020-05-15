package me.twodee.friendlyneighbor;

import io.grpc.stub.StreamObserver;
import me.twodee.friendlyneighbor.dto.ResultObject;
import me.twodee.friendlyneighbor.dto.UserLocationsResult;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.service.Discovery;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FnCoreHandler extends FnCoreGrpc.FnCoreImplBase
{
    private final Discovery discovery;

    @Inject
    public FnCoreHandler(Discovery discovery)
    {
        this.discovery = discovery;
    }

    @Override
    public void saveUserLocation(FnCoreGenerated.RegistrationRequest request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {
        ResultObject result = discovery.saveUserLocation(buildSearchLocation(request));
        responseObserver.onNext(buildRequestResult(result));
        responseObserver.onCompleted();
    }


    @Override
    public void deleteUserLocation(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {
        ResultObject result = discovery.deleteUserLocation(request.getUserId());
        responseObserver.onNext(buildRequestResult(result));
        responseObserver.onCompleted();
    }

    private UserLocation buildSearchLocation(FnCoreGenerated.RegistrationRequest request)
    {
        return new UserLocation(request.getUserId(),
                                new UserLocation.Position(request.getLocation().getLatitude(),
                                                          request.getLocation().getLongitude()),
                                request.getRadius());
    }

    private UserLocation buildSearchLocation(FnCoreGenerated.SearchAreaRequest request)
    {
        return new UserLocation(request.getUserId(),
                                new UserLocation.Position(request.getLocation().getLatitude(),
                                                          request.getLocation().getLongitude()),
                                request.getRadius());
    }

    private FnCoreGenerated.RequestResult buildRequestResult(ResultObject result)
    {
        return FnCoreGenerated.RequestResult.newBuilder().setSuccess(true).build();
    }

    @Override
    public void findUsersInCircleByLocation(FnCoreGenerated.SearchAreaRequest request, StreamObserver<FnCoreGenerated.NearbyUsersResult> responseObserver)
    {
        UserLocationsResult result = discovery.lookupNearbyUsersByLocation(buildSearchLocation(request));

        responseObserver.onNext(buildLocationsResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void findUsersInCircleById(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.NearbyUsersResult> responseObserver)
    {
        UserLocationsResult result = discovery.lookupNearbyUsersByUserId(request.getUserId());

        responseObserver.onNext(buildLocationsResult(result));
        responseObserver.onCompleted();
    }

    private FnCoreGenerated.NearbyUsersResult buildLocationsResult(UserLocationsResult results)
    {
        if (results.getNotification().hasErrors()) {
            return buildFailedResult(results.getNotification().getErrors());
        }

        return FnCoreGenerated.NearbyUsersResult.newBuilder()
                .setMetaResult(FnCoreGenerated.RequestResult.newBuilder().setSuccess(true).build())
                .addAllUser(buildUserList(results.getUserLocations()))
                .build();
    }

    private List<FnCoreGenerated.UserNearby> buildUserList(List<UserLocation> users)
    {
        return users.stream()
                .map(this::createPbUser)
                .collect(Collectors.toList());
    }

    private FnCoreGenerated.UserNearby createPbUser(UserLocation userLocation)
    {
        return FnCoreGenerated.UserNearby.newBuilder()
                .setDistance(userLocation.getDistance().doubleValue())
                .setUserId(userLocation.getId())
                .build();
    }

    private FnCoreGenerated.NearbyUsersResult buildFailedResult(Map<String, String> errors)
    {
        return FnCoreGenerated.NearbyUsersResult
                .newBuilder()
                .setMetaResult(FnCoreGenerated.RequestResult.newBuilder()
                                       .setSuccess(false)
                                       .putAllErrors(errors)
                                       .build()).build();
    }
}
