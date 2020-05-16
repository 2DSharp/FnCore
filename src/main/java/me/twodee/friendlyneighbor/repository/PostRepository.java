package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;

import java.util.List;

public interface PostRepository
{
    Post save(Post post);

    void forwardToUsers(List<UserLocation> userLocations, Post post);

    List<String> findAllForUser(String userId, List<UserLocation> nearbyUsers);
}
