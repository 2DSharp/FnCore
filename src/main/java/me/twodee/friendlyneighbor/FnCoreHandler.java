package me.twodee.friendlyneighbor;

import io.grpc.stub.StreamObserver;
import me.twodee.friendlyneighbor.dto.PostResults;
import me.twodee.friendlyneighbor.dto.ResultObject;
import me.twodee.friendlyneighbor.dto.UserLocationsResult;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.service.Discovery;
import me.twodee.friendlyneighbor.service.Feed;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FnCoreHandler extends FnCoreGrpc.FnCoreImplBase
{
    private final Discovery discovery;
    private final Feed feed;

    @Inject
    public FnCoreHandler(Discovery discovery, Feed feed)
    {
        this.discovery = discovery;
        this.feed = feed;
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

    private UserLocation buildSearchLocation(FnCoreGenerated.PostData request)
    {
        return new UserLocation(request.getUserId(),
                                new UserLocation.Position(request.getLocation().getLatitude(),
                                                          request.getLocation().getLongitude()),
                                request.getRadius());
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
        return FnCoreGenerated.RequestResult.newBuilder().setSuccess(
                !result.getNotification().hasErrors()).putAllErrors(result.getNotification().getErrors()).build();
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

    @Override
    public void forwardRequestNearbyDefaultLocation(FnCoreGenerated.PostData request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {
        ResultObject result = feed.pushRequestToNearbyUsers(request.getPostId(), request.getUserId());
        responseObserver.onNext(buildRequestResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void forwardRequestNearbyCustomLocation(FnCoreGenerated.PostData request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {
        ResultObject result = feed.pushRequestToNearbyUsers(request.getPostId(), buildSearchLocation(request));
        responseObserver.onNext(buildRequestResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void fetchRequestsNearby(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.RequestsNearby> responseObserver)
    {
        PostResults posts = feed.fetchRequestsForUser(request.getUserId());
        responseObserver.onNext(buildRequestsNearbyResult(posts));
        responseObserver.onCompleted();
    }


    private FnCoreGenerated.RequestsNearby buildRequestsNearbyResult(PostResults posts)
    {
        if (posts.getNotification().hasErrors()) {
            return FnCoreGenerated.RequestsNearby.newBuilder()
                    .setMetaResult(buildMetaResult(posts.getNotification().getErrors()))
                    .build();
        }

        return FnCoreGenerated.RequestsNearby.newBuilder()
                .setMetaResult(FnCoreGenerated.RequestResult.newBuilder().setSuccess(true).build())
                .addAllRequests(buildNearbyRequestList(posts.getPosts()))
                .build();

    }

    private List<FnCoreGenerated.PostOutput> buildNearbyRequestList(List<Post> posts)
    {
        return posts.stream().map(this::createPbPost).collect(Collectors.toList());
    }

    private FnCoreGenerated.PostOutput createPbPost(Post post)
    {
        return FnCoreGenerated.PostOutput.newBuilder()
                .setPostId(post.getId())
                .setDistance(post.getLocation().getDistance().doubleValue())
                .build();
    }


    private FnCoreGenerated.NearbyUsersResult buildLocationsResult(UserLocationsResult results)
    {
        if (results.getNotification().hasErrors()) {
            return buildFailedNearbyUsersResult(results.getNotification().getErrors());
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

    private FnCoreGenerated.RequestResult buildMetaResult(Map<String, String> errors)
    {
        return FnCoreGenerated.RequestResult.newBuilder()
                .setSuccess(false)
                .putAllErrors(errors)
                .build();
    }

    private FnCoreGenerated.NearbyUsersResult buildFailedNearbyUsersResult(Map<String, String> errors)
    {
        return FnCoreGenerated.NearbyUsersResult
                .newBuilder()
                .setMetaResult(buildMetaResult(errors))
                .build();
    }
}
