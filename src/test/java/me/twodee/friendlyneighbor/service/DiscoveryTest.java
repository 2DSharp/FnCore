package me.twodee.friendlyneighbor.service;

import me.twodee.friendlyneighbor.FnCoreGenerated;
import me.twodee.friendlyneighbor.dto.UserLocationsDTO;
import me.twodee.friendlyneighbor.entity.UserLocation;
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

    FnCoreGenerated.SearchAreaRequest getRequest()
    {
        FnCoreGenerated.Location location = FnCoreGenerated.Location.newBuilder()
                .setLatitude(22.0)
                .setLongitude(77.0)
                .build();

        return FnCoreGenerated.SearchAreaRequest.newBuilder()
                .setLocation(location)
                .setRadius(2)
                .setUserId("test")
                .build();
    }

    @Test
    void testInvalidUserOnLookupByLocation() throws InvalidUser
    {
        when(repository.getUsersNearBy(Mockito.any(UserLocation.class))).thenThrow(InvalidUser.class);
        Discovery discovery = new Discovery(repository);

        UserLocation loc = new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100);

        UserLocationsDTO result = discovery.lookupNearbyUsersByLocation(loc);

        assertTrue(result.getNotification().hasErrors());
        assertTrue(result.getNotification().getErrors().containsKey("userId"));
        assertNull(result.getUserLocations());
    }

    @Test
    void testLookupByLocationSuccessful() throws InvalidUser
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation resultLoc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        resultLoc.setDis(100);
        usersList.add(resultLoc);

        resultLoc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        resultLoc.setDis(2.0);
        usersList.add(resultLoc);

        when(repository.getUsersNearBy(Mockito.any(UserLocation.class))).thenReturn(usersList);
        Discovery discovery = new Discovery(repository);

        UserLocation searchLoc = new UserLocation("abc123", new UserLocation.Position(22.507449, 88.34), 2100);

        UserLocationsDTO nearbyUsers = discovery.lookupNearbyUsersByLocation(searchLoc);

        assertFalse(nearbyUsers.getNotification().hasErrors());
        assertThat(nearbyUsers.getUserLocations().size(), equalTo(2));
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("id")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("dis")
                .containsOnly(100, 2.0);
    }

    @Test
    void testInvalidUserOnLookupById() throws InvalidUser
    {
        when(repository.getUsersNearBy(Mockito.anyString())).thenThrow(InvalidUser.class);
        Discovery discovery = new Discovery(repository);
        String requestingUid = "abc123";

        UserLocationsDTO result = discovery.lookupNearbyUsersByUserId(requestingUid);

        assertTrue(result.getNotification().hasErrors());
        assertFalse(result.getNotification().getErrors().isEmpty());
        assertTrue(result.getNotification().getErrors().containsKey("userId"));
        assertNull(result.getUserLocations());
    }

    @Test
    void testLookupByIdSuccessful() throws InvalidUser
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation loc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        loc.setDis(10);
        usersList.add(loc);

        loc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        loc.setDis(2.0);
        usersList.add(loc);

        when(repository.getUsersNearBy(Mockito.anyString())).thenReturn(usersList);
        Discovery discovery = new Discovery(repository);
        String requestingUid = "abc123";

        UserLocationsDTO nearbyUsers = discovery.lookupNearbyUsersByUserId(requestingUid);

        assertFalse(nearbyUsers.getNotification().hasErrors());
        assertThat(nearbyUsers.getUserLocations().size(), equalTo(2));
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("id")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(nearbyUsers.getUserLocations()).extracting("dis")
                .containsOnly(10, 2.0);
    }
}