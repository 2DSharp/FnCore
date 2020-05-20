package me.twodee.friendlyneighbor.service;

import me.twodee.friendlyneighbor.dto.ResultObject;
import me.twodee.friendlyneighbor.dto.UserLocationResult;
import me.twodee.friendlyneighbor.dto.UserLocationsResult;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.exception.DbFailure;
import me.twodee.friendlyneighbor.exception.InvalidUser;
import me.twodee.friendlyneighbor.repository.LocationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class DiscoveryTest
{
    @Mock
    LocationRepository repository;

    @BeforeEach
    void initMock()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testInvalidUserOnLookupByLocation() throws InvalidUser, DbFailure
    {
        when(repository.getUsersNearBy(Mockito.any(UserLocation.class))).thenThrow(InvalidUser.class);
        Discovery discovery = new Discovery(repository);

        UserLocation loc = new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100);

        UserLocationsResult result = discovery.lookupNearbyUsersByLocation(loc);

        assertTrue(result.getNotification().hasErrors());
        assertTrue(result.getNotification().getErrors().containsKey("userId"));
        assertNull(result.getUserLocations());
    }

    @Test
    void testLookupByLocationSuccessful() throws InvalidUser, DbFailure
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation resultLoc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        resultLoc.setDistance(100);
        usersList.add(resultLoc);

        resultLoc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        resultLoc.setDistance(2.0);
        usersList.add(resultLoc);

        when(repository.getUsersNearBy(Mockito.any(UserLocation.class))).thenReturn(usersList);
        Discovery discovery = new Discovery(repository);

        UserLocation searchLoc = new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100);

        UserLocationsResult nearbyUsers = discovery.lookupNearbyUsersByLocation(searchLoc);

        assertFalse(nearbyUsers.getNotification().hasErrors());
        assertThat(nearbyUsers.getUserLocations().size(), equalTo(2));
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("id")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("distance")
                .containsOnly(100, 2.0);
    }

    @Test
    void testInvalidUserOnLookupById() throws InvalidUser, DbFailure
    {
        when(repository.getUsersNearBy(Mockito.anyString())).thenThrow(InvalidUser.class);
        Discovery discovery = new Discovery(repository);
        String requestingUid = "abc123";

        UserLocationsResult result = discovery.lookupNearbyUsersByUserId(requestingUid);

        assertTrue(result.getNotification().hasErrors());
        assertFalse(result.getNotification().getErrors().isEmpty());
        assertTrue(result.getNotification().getErrors().containsKey("userId"));
        assertNull(result.getUserLocations());
    }

    @Test
    void testLookupByIdSuccessful() throws InvalidUser, DbFailure
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation loc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        loc.setDistance(10);
        usersList.add(loc);

        loc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        loc.setDistance(2.0);
        usersList.add(loc);

        when(repository.getUsersNearBy(Mockito.anyString())).thenReturn(usersList);
        Discovery discovery = new Discovery(repository);
        String requestingUid = "abc123";

        UserLocationsResult nearbyUsers = discovery.lookupNearbyUsersByUserId(requestingUid);

        assertFalse(nearbyUsers.getNotification().hasErrors());
        assertThat(nearbyUsers.getUserLocations().size(), equalTo(2));
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("id")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("distance")
                .containsOnly(10, 2.0);
    }

    @Test
    void testUserSave()
    {
        Discovery discovery = new Discovery(repository);
        UserLocation loc = new UserLocation("abc", new UserLocation.Position(10, 10), 10);

        ResultObject res = discovery.saveUserLocation(loc);

        assertFalse(res.getNotification().hasErrors());
    }

    @Test
    void testUserSaveFailure()
    {
        UserLocation loc = new UserLocation("abc", new UserLocation.Position(10, 10), 10);

        when(repository.save(any())).thenThrow(RuntimeException.class);
        Discovery discovery = new Discovery(repository);

        ResultObject res = discovery.saveUserLocation(loc);

        assertTrue(res.getNotification().hasErrors());
        assertThat(res.getNotification().getErrors().get("internal"), equalTo(ResultObject.SOMETHING_WENT_WRONG));
    }

    @Test
    void testUserDelete()
    {
        Discovery discovery = new Discovery(repository);

        ResultObject res = discovery.deleteUserLocation("abc");

        assertFalse(res.getNotification().hasErrors());
    }

    @Test
    void testUserDeleteFailure()
    {
        doThrow(RuntimeException.class).when(repository).deleteById(any());
        Discovery discovery = new Discovery(repository);

        ResultObject res = discovery.deleteUserLocation("abc");

        assertTrue(res.getNotification().hasErrors());
        assertThat(res.getNotification().getErrors().get("internal"), equalTo(ResultObject.SOMETHING_WENT_WRONG));
    }

    @Test
    void testGetUserLocationSuccess()
    {
        when(repository.findById("abc")).thenReturn(new UserLocation("abc", new UserLocation.Position(2, 3), 2.3));
        Discovery discovery = new Discovery(repository);
        UserLocationResult result = discovery.getUserLocation("abc");

        assertFalse(result.getNotification().hasErrors());
        assertThat(result.userLocation.getId(), equalTo("abc"));
        assertThat(result.userLocation.getRadius(), equalTo(2.3));
    }

    @Test
    void testGetUserLocationFailure()
    {
        when(repository.findById(any())).thenReturn(null);
        Discovery discovery = new Discovery(repository);
        UserLocationResult result = discovery.getUserLocation("abc");
        assertTrue(result.getNotification().hasErrors());
        assertTrue(result.getNotification().getErrors().containsKey("userId"));
    }
}