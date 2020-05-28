package me.twodee.friendlyneighbor;

import io.grpc.stub.StreamObserver;
import me.twodee.friendlyneighbor.dto.*;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.service.Discovery;
import me.twodee.friendlyneighbor.service.Feed;
import me.twodee.friendlyneighbor.service.Notifier;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FnCoreHandler extends FnCoreGrpc.FnCoreImplBase {
    private final Discovery discovery;
    private final Feed feed;
    private Notifier notifier;

    /**
     * @param discovery
     * @param feed
     * @deprecated
     */
    FnCoreHandler(Discovery discovery, Feed feed) {
        this.discovery = discovery;
        this.feed = feed;
    }

    @Inject
    FnCoreHandler(Discovery discovery, Feed feed, Notifier notifier) {
        this.discovery = discovery;
        this.feed = feed;
        this.notifier = notifier;
    }

    @Override
    public void saveUserLocation(FnCoreGenerated.RegistrationRequest request, StreamObserver<FnCoreGenerated.Result> responseObserver) {
        ResultObject result = discovery.saveUserLocation(buildSearchLocation(request));
        responseObserver.onNext(buildResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteUserLocation(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.Result> responseObserver)
    {
        ResultObject result = discovery.deleteUserLocation(request.getUserId());
        responseObserver.onNext(buildResult(result));
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

    private FnCoreGenerated.Result buildResult(ResultObject result)
    {
        return FnCoreGenerated.Result.newBuilder().setSuccess(
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
    public void findUsersInCircleById(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.NearbyUsersResult> responseObserver) {
        UserLocationsResult result = discovery.lookupNearbyUsersByUserId(request.getUserId());

        responseObserver.onNext(buildLocationsResult(result));
        responseObserver.onCompleted();
    }

    private Post.PostType generateDomainType(FnCoreGenerated.PostData.Type type) {
        switch (type) {
            case REQUEST:
                return Post.PostType.REQUEST;
            case OFFERING:
                return Post.PostType.OFFERING;
        }
        return Post.PostType.REQUEST;
    }

    @Override
    public void forwardRequestNearbyDefaultLocation(FnCoreGenerated.PostData request, StreamObserver<FnCoreGenerated.Result> responseObserver) {
        ResultObject result = feed.fanoutToNearbyUsers(new PostData(request.getPostId(), request.getTitle(),
                                                                    generateDomainType(request.getType())),
                                                       request.getUserId());
        responseObserver.onNext(buildResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void forwardRequestNearbyCustomLocation(FnCoreGenerated.PostData request, StreamObserver<FnCoreGenerated.Result> responseObserver) {
        ResultObject result = feed.fanoutToNearbyUsers(new PostData(request.getPostId(), request.getTitle(),
                                                                    generateDomainType(request.getType())),
                                                       buildSearchLocation(request));
        responseObserver.onNext(buildResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void fetchRequestsNearby(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.RequestsNearby> responseObserver)
    {
        PostResults posts = feed.fetchRequestsForUser(request.getUserId());
        responseObserver.onNext(buildRequestsNearbyResult(posts));
        responseObserver.onCompleted();
    }

    @Override
    public void getUserLocation(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.LocationRadiusResult> responseObserver)
    {
        UserLocationResult result = discovery.getUserLocation(request.getUserId());
        responseObserver.onNext(buildUserLocationResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteRequest(FnCoreGenerated.PostData request, StreamObserver<FnCoreGenerated.Result> responseObserver) {
        ResultObject result = feed.delete(request.getPostId());
        responseObserver.onNext(buildResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void saveUserForNotifications(FnCoreGenerated.NotificationIdentifier request, StreamObserver<FnCoreGenerated.Result> responseObserver) {
        ResultObject result = notifier.saveToNotification(request.getUserId(), request.getNotifyToken());
        responseObserver.onNext(buildResult(result));
        responseObserver.onCompleted();
    }

    @Override
    public void notifyForResponse(FnCoreGenerated.ResponseNotification request, StreamObserver<FnCoreGenerated.Result> responseObserver) {
        System.out.println(request.getResponseType().getNumber());

        ResultObject result = notifier.sendNewResponseNotification(request.getUserId(),
                                                                   request.getNameOfRespondingUser(),
                                                                   generateDomainResponseType(request.getResponseType())
        );
        responseObserver.onNext(buildResult(result));
        responseObserver.onCompleted();
    }

    private Notifier.ResponseType generateDomainResponseType(FnCoreGenerated.ResponseNotification.Type type) {
        switch (type) {
            case ACCEPT:
                return Notifier.ResponseType.ACCEPT;
            default:
                return Notifier.ResponseType.RESPOND;
        }
    }

    private FnCoreGenerated.LocationRadiusResult buildUserLocationResult(UserLocationResult result) {
        if (result.getNotification().hasErrors()) {
            return FnCoreGenerated.LocationRadiusResult.newBuilder()
                    .setMetaResult(buildMetaResult(result.getNotification().getErrors()))
                    .build();
        }
        UserLocation location = result.userLocation;

        return FnCoreGenerated.LocationRadiusResult.newBuilder()
                .setLocation(
                        FnCoreGenerated.Location.newBuilder()
                                .setLatitude(location.getPosition().getLatitude())
                                .setLongitude(location.getPosition().getLongitude())
                                .build()
                )
                .setRadius(location.getRadius())
                .setMetaResult(FnCoreGenerated.Result.newBuilder().setSuccess(true).build())
                .build();
    }

    private FnCoreGenerated.RequestsNearby buildRequestsNearbyResult(PostResults posts)
    {
        if (posts.getNotification().hasErrors()) {
            return FnCoreGenerated.RequestsNearby.newBuilder()
                    .setMetaResult(buildMetaResult(posts.getNotification().getErrors()))
                    .build();
        }

        return FnCoreGenerated.RequestsNearby.newBuilder()
                .setMetaResult(FnCoreGenerated.Result.newBuilder().setSuccess(true).build())
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
                .setMetaResult(FnCoreGenerated.Result.newBuilder().setSuccess(true).build())
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

    private FnCoreGenerated.Result buildMetaResult(Map<String, String> errors)
    {
        return FnCoreGenerated.Result.newBuilder()
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
