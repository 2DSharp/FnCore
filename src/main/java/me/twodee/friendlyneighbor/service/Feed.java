package me.twodee.friendlyneighbor.service;

import lombok.extern.java.Log;
import me.twodee.friendlyneighbor.dto.*;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.repository.PostRepository;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log
public class Feed {
    private final Discovery discovery;
    private final PostRepository repository;
    private Notifier notifier;

    Feed(Discovery discovery, PostRepository repository) {
        this.discovery = discovery;
        this.repository = repository;
    }

    @Inject
    Feed(Discovery discovery, PostRepository repository, Notifier notifier) {
        this.discovery = discovery;
        this.repository = repository;
        this.notifier = notifier;
    }

    public ResultObject fanoutToNearbyUsers(PostData data, UserLocation userLocation) {
        if (userLocation.getRadius() == 0 || userLocation.getPosition().getLatitude() == 0 || userLocation.getPosition().getLongitude() == 0) {
            return new ResultObject("location", "Location coordinates and/or radius haven't been set.");
        }
        Post persistedPost = savePost(userLocation, data);
        return fanout(persistedPost);
    }

    public ResultObject fanoutToNearbyUsers(PostData data, String userId) {
        UserLocationResult result = discovery.getUserLocation(userId);
        if (result.getNotification().hasErrors()) {
            return new ResultObject(result.getNotification().getErrors());
        }
        UserLocation currentUserLocation = result.userLocation;
        Post persistedPost = savePost(currentUserLocation, data);
        return fanout(persistedPost);
    }

    public ResultObject pushRequestToNearbyUsers(String postId, UserLocation currentUserLocation) {
        if (currentUserLocation.getRadius() == 0 || currentUserLocation.getPosition().getLatitude() == 0 || currentUserLocation.getPosition().getLongitude() == 0) {
            return new ResultObject("location", "Location coordinates and/or radius haven't been set.");
        }
        return saveAndPush(currentUserLocation, new Post(postId, currentUserLocation, LocalDateTime.now()));
    }

    public ResultObject pushRequestToNearbyUsers(String postId, String userId)
    {
        UserLocationResult result = discovery.getUserLocation(userId);
        if (result.getNotification().hasErrors()) {
            return new ResultObject(result.getNotification().getErrors());
        }
        UserLocation currentUserLocation = result.userLocation;
        return saveAndPush(currentUserLocation, new Post(postId, currentUserLocation, LocalDateTime.now()));
    }

    public PostResults fetchRequestsForUser(String userId)
    {
        UserLocationResult locationResult = discovery.getUserLocation(userId);
        if (locationResult.getNotification().hasErrors()) {
            PostResults results = new PostResults();
            results.setNotification(locationResult.getNotification());
            return results;
        }

        return new PostResults(repository.findAllForUser(locationResult.userLocation));
    }

    public ResultObject saveNotificationRecipient(String id, String token) {
        notifier.saveToNotification(id, token);
        return new SuccessResult();
    }

    public ResultObject sendNotificationForNewResponse(String id, String nameOfRespondingUser, Notifier.ResponseType type) {
        notifier.sendNewResponseNotification(id, nameOfRespondingUser, type);
        return new SuccessResult();
    }

    private ResultObject saveAndPush(UserLocation currentUserLocation, Post post) {
        Post persistedPost = repository.save(post);
        UserLocationsResult usersNearby = discovery.lookupNearbyUsersByLocation(currentUserLocation);
        if (usersNearby.getNotification().hasErrors()) {
            return new ResultObject(usersNearby.getNotification().getErrors());
        }
        repository.forwardToUsers(usersNearby.getUserLocations(), persistedPost);
        return new SuccessResult();
    }

    private Post savePost(UserLocation currentUserLocation, PostData postData) {
        Post post = new Post(postData.postId, currentUserLocation, LocalDateTime.now());
        post.setTitle(postData.title);
        post.setType(postData.type);
        return repository.save(post);
    }

    private ResultObject fanout(Post post) {
        UserLocationsResult usersNearby = discovery.lookupNearbyUsersByLocation(post.getLocation());
        if (usersNearby.getNotification().hasErrors()) {
            return new ResultObject(usersNearby.getNotification().getErrors());
        }
        repository.forwardToUsers(usersNearby.getUserLocations(), post);
        CompletableFuture.runAsync(() -> notifyUsersWithSimilarPosts(usersNearby.getUserLocations(), post));

        return new SuccessResult();
    }

    private void notifyUsersWithSimilarPosts(List<UserLocation> usersNearby, Post post) {
        List<Post> posts = repository.fetchMatchingNearbyPosts(post.getLocation(), usersNearby, post);
        notifier.sendPostRecommendation(posts.stream().map(p -> p.getLocation().getId()).collect(Collectors.toList()));
    }

    public ResultObject delete(String postId) {
        repository.deleteById(postId);
        return new SuccessResult();
    }

}
