package me.twodee.friendlyneighbor.service;

import me.twodee.friendlyneighbor.FnCoreGenerated;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        FnCoreGenerated.NearbyUsersResult result = discovery.lookupNearbyUsersByLocation(getRequest());

        assertFalse(result.getMetaResult().getSuccess());
        assertFalse(result.getMetaResult().getErrorsMap().isEmpty());
        assertTrue(result.getMetaResult().getErrorsMap().containsKey("userId"));
        assertTrue(result.getUserList().isEmpty());
    }

    @Test
    void testLookupByLocationSuccessful() throws InvalidUser
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation loc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        loc.setDis(100);
        usersList.add(loc);

        loc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        loc.setDis(2);
        usersList.add(loc);

        when(repository.getUsersNearBy(Mockito.any(UserLocation.class))).thenReturn(usersList);
        Discovery discovery = new Discovery(repository);

        FnCoreGenerated.NearbyUsersResult nearbyUsers = discovery.lookupNearbyUsersByLocation(getRequest());

        assertTrue(nearbyUsers.getMetaResult().getSuccess());
        assertThat(nearbyUsers.getUserCount(), equalTo(2));
        Assertions.assertThat(nearbyUsers.getUserList()).extracting("userId")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(nearbyUsers.getUserList()).extracting("distance")
                .containsOnly(100.0, 2.0);
    }

    @Test
    void testInvalidUserOnLookupById() throws InvalidUser
    {
        FnCoreGenerated.UserIdentifier request = FnCoreGenerated.UserIdentifier.newBuilder().build();
        when(repository.getUsersNearBy(Mockito.anyString())).thenThrow(InvalidUser.class);
        Discovery discovery = new Discovery(repository);


        FnCoreGenerated.NearbyUsersResult result = discovery.lookupNearbyUsersByUserId(request);

        assertFalse(result.getMetaResult().getSuccess());
        assertFalse(result.getMetaResult().getErrorsMap().isEmpty());
        assertTrue(result.getMetaResult().getErrorsMap().containsKey("userId"));
        assertTrue(result.getUserList().isEmpty());
    }

    @Test
    void testLookupByIdSuccessful() throws InvalidUser
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation loc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        loc.setDis(10);
        usersList.add(loc);

        loc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        loc.setDis(20);
        usersList.add(loc);

        when(repository.getUsersNearBy(Mockito.anyString())).thenReturn(usersList);
        Discovery discovery = new Discovery(repository);
        FnCoreGenerated.UserIdentifier request = FnCoreGenerated.UserIdentifier.newBuilder().build();

        FnCoreGenerated.NearbyUsersResult nearbyUsers = discovery.lookupNearbyUsersByUserId(request);

        assertTrue(nearbyUsers.getMetaResult().getSuccess());
        assertThat(nearbyUsers.getUserCount(), equalTo(2));
        Assertions.assertThat(nearbyUsers.getUserList()).extracting("userId")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(nearbyUsers.getUserList()).extracting("distance")
                .containsOnly(10.0, 20.0);
    }
}