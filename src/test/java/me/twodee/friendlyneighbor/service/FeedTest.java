package me.twodee.friendlyneighbor.service;

import me.twodee.friendlyneighbor.dto.*;
import me.twodee.friendlyneighbor.entity.Post;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FeedTest
{
    @Mock
    Discovery discovery;

    @Mock
    PostRepository repository;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void fanoutWithCustomLocationNoLocationPassed()
    {
        Feed feed = new Feed(discovery, repository);
        ResultObject result = feed.pushRequestToNearbyUsers("p1",
                                                            new UserLocation());
        assertTrue(result.getNotification().hasErrors());
        assertThat(result.getNotification().getErrors().get("location"),
                   equalTo("Location coordinates and/or radius haven't been set."));
    }

    @Test
    void fanoutWithCustomLocationZeroLocation()
    {
        Feed feed = new Feed(discovery, repository);
        ResultObject result = feed.pushRequestToNearbyUsers("p1",
                                                            new UserLocation("x1", new UserLocation.Position(0, 0),
                                                                             10));
        assertTrue(result.getNotification().hasErrors());
        assertThat(result.getNotification().getErrors().get("location"),
                   equalTo("Location coordinates and/or radius haven't been set."));
    }

    @Test
    void fanoutWithCustomLocationZeroRadius()
    {
        Feed feed = new Feed(discovery, repository);
        ResultObject result = feed.pushRequestToNearbyUsers("p1",
                                                            new UserLocation("x1", new UserLocation.Position(20, 20),
                                                                             0));
        assertTrue(result.getNotification().hasErrors());
        assertThat(result.getNotification().getErrors().get("location"),
                   equalTo("Location coordinates and/or radius haven't been set."));
    }

    @Test
    void fanoutWithCustomLocationSuccessful()
    {
        Feed feed = new Feed(discovery, repository);
        when(discovery.lookupNearbyUsersByLocation(any())).thenReturn(new UserLocationsResult(new ArrayList<>()));
        when(repository.save(any())).thenReturn(new Post());
        ResultObject result = feed.pushRequestToNearbyUsers("p1",
                                                            new UserLocation("test", new UserLocation.Position(23, 20),
                                                                             10));
        assertFalse(result.getNotification().hasErrors());
    }

    @Test
    void fanoutWithCustomLocationInvalidUserId()
    {
        Feed feed = new Feed(discovery, repository);
        Notification note = new Notification();
        note.addError("a", "b");
        when(discovery.lookupNearbyUsersByLocation(any())).thenReturn(new UserLocationsResult(note));
        when(repository.save(any())).thenReturn(new Post());

        ResultObject result = feed.pushRequestToNearbyUsers("p1", new UserLocation());

        assertTrue(result.getNotification().hasErrors());
    }

    @Test
    void fanoutWithUserIdSuccessful()
    {
        Feed feed = new Feed(discovery, repository);
        when(discovery.getUserLocation(any())).thenReturn(new UserLocationResult());
        when(discovery.lookupNearbyUsersByLocation(any())).thenReturn(new UserLocationsResult(new ArrayList<>()));
        when(repository.save(any())).thenReturn(new Post());

        ResultObject result = feed.pushRequestToNearbyUsers("p1", "uid");

        assertFalse(result.getNotification().hasErrors());
    }

    @Test
    void fanoutWithUserIdInvalidUserId_Check1()
    {
        Feed feed = new Feed(discovery, repository);
        Notification note = new Notification();
        note.addError("a", "b");
        UserLocationResult userLocation = new UserLocationResult();
        userLocation.setNotification(note);
        when(discovery.getUserLocation(any())).thenReturn(userLocation);
        when(discovery.lookupNearbyUsersByLocation(any())).thenReturn(new UserLocationsResult(new ArrayList<>()));
        when(repository.save(any())).thenReturn(new Post());

        ResultObject result = feed.pushRequestToNearbyUsers("p1", "uid");

        assertTrue(result.getNotification().hasErrors());
        assertTrue(result.getNotification().getErrors().containsKey("a"));
    }

    @Test
    void fanoutWithUserIdInvalidUserId_Check2()
    {
        Feed feed = new Feed(discovery, repository);
        Notification note = new Notification();
        note.addError("a", "b");
        UserLocationResult userLocation = new UserLocationResult();
        when(discovery.getUserLocation(any())).thenReturn(userLocation);
        when(discovery.lookupNearbyUsersByLocation(any())).thenReturn(new UserLocationsResult(note));
        when(repository.save(any())).thenReturn(new Post());

        ResultObject result = feed.pushRequestToNearbyUsers("p1", "uid");

        assertTrue(result.getNotification().hasErrors());
        assertTrue(result.getNotification().getErrors().containsKey("a"));
    }

    @Test
    void fetchRequestsSuccessful()
    {
        Feed feed = new Feed(discovery, repository);
        when(discovery.lookupNearbyUsersByUserId(any())).thenReturn(new UserLocationsResult(new ArrayList<>()));
        when(repository.findAllForUser(any(), any())).thenReturn(new ArrayList<>());
        when(discovery.getUserLocation(any())).thenReturn(new UserLocationResult(new UserLocation()));
        PostResults results = feed.fetchRequestsForUser("uid");

        assertFalse(results.getNotification().hasErrors());
        assertNotNull(results.getPosts());
    }

    @Test
    void fetchRequestsInvalidId()
    {
        Feed feed = new Feed(discovery, repository);
        Notification note = new Notification();
        note.addError("a", "b");
        when(discovery.lookupNearbyUsersByUserId(any())).thenReturn(new UserLocationsResult(note));
        PostResults results = feed.fetchRequestsForUser("uid");

        assertTrue(results.getNotification().hasErrors());
        assertTrue(results.getNotification().getErrors().containsKey("a"));
        assertNull(results.getPosts());
    }

    @Test
    void fetchRequestsEmptyFeed()
    {
        Feed feed = new Feed(discovery, repository);
        when(discovery.lookupNearbyUsersByUserId(any())).thenReturn(new UserLocationsResult(new ArrayList<>()));
        when(discovery.getUserLocation(any())).thenReturn(new UserLocationResult(new UserLocation()));
        PostResults results = feed.fetchRequestsForUser("uid");

        assertFalse(results.getNotification().hasErrors());
        assertTrue(results.getPosts().isEmpty());
    }
}