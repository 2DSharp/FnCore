package me.twodee.friendlyneighbor.repository;

import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;

import java.util.List;

public interface PostRepository {
    Post save(Post post);

    void forwardToUsers(List<UserLocation> userLocations, Post post);

    /**
     * @param currentUserLocation
     * @param nearbyUsers
     * @return
     * @deprecated
     */
    List<Post> findAllForUser(UserLocation currentUserLocation, List<UserLocation> nearbyUsers);

    List<Post> findAllForUser(UserLocation currentUserLocation);

    List<Post> fetchMatchingNearbyPosts(UserLocation currentUserLocation, List<UserLocation> nearbyUsers, Post post);

    void deleteById(String id);
}
