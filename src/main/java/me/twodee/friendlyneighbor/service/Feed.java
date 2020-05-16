package me.twodee.friendlyneighbor.service;

import lombok.extern.java.Log;
import me.twodee.friendlyneighbor.dto.PostResults;
import me.twodee.friendlyneighbor.dto.ResultObject;
import me.twodee.friendlyneighbor.dto.SuccessResult;
import me.twodee.friendlyneighbor.dto.UserLocationsResult;
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
    public Feed(Discovery discovery, PostRepository repository)
    {
        this.discovery = discovery;
        this.repository = repository;
    }

    public ResultObject pushRequestToNearbyUsers(String postId, UserLocation currentUserLocation)
    {
        saveAndPush(currentUserLocation, new Post(postId, currentUserLocation, LocalDateTime.now()));
        return new SuccessResult();
    }

    public ResultObject pushRequestToNearbyUsers(String postId, String userId)
    {
        UserLocation currentUserLocation = discovery.getUserLocation(userId);
        saveAndPush(currentUserLocation, new Post(postId, currentUserLocation, LocalDateTime.now()));
        return new SuccessResult();
    }

    public PostResults fetchRequestsForUser(String userId)
    {
        return new PostResults(
                repository.findAllForUser(userId, discovery.lookupNearbyUsersByUserId(userId).getUserLocations()));

    }

    private void saveAndPush(UserLocation currentUserLocation, Post post)
    {
        Post persistedPost = repository.save(post);
        UserLocationsResult usersNearby = discovery.lookupNearbyUsersByLocation(currentUserLocation);
        repository.forwardToUsers(usersNearby.getUserLocations(), persistedPost);
    }
}
