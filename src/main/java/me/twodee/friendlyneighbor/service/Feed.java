package me.twodee.friendlyneighbor.service;

import lombok.extern.java.Log;
import me.twodee.friendlyneighbor.dto.*;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.repository.PostRepository;

import javax.inject.Inject;
import java.time.LocalDateTime;

@Log
public class Feed
{
    private final Discovery discovery;
    private final PostRepository repository;

    @Inject
    Feed(Discovery discovery, PostRepository repository)
    {
        this.discovery = discovery;
        this.repository = repository;
    }

    public ResultObject pushRequestToNearbyUsers(String postId, UserLocation currentUserLocation)
    {
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
        UserLocationsResult usersNearby = discovery.lookupNearbyUsersByUserId(userId);
        if (usersNearby.getNotification().hasErrors()) {
            PostResults results = new PostResults();
            results.setNotification(usersNearby.getNotification());
            return results;
        }

        return new PostResults(repository.findAllForUser(userId, usersNearby.getUserLocations()));
    }

    private ResultObject saveAndPush(UserLocation currentUserLocation, Post post)
    {
        Post persistedPost = repository.save(post);
        UserLocationsResult usersNearby = discovery.lookupNearbyUsersByLocation(currentUserLocation);
        if (usersNearby.getNotification().hasErrors()) {
            return new ResultObject(usersNearby.getNotification().getErrors());
        }

        repository.forwardToUsers(usersNearby.getUserLocations(), persistedPost);
        return new SuccessResult();
    }
}
