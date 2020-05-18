package me.twodee.friendlyneighbor;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import me.twodee.friendlyneighbor.dto.Notification;
import me.twodee.friendlyneighbor.dto.ResultObject;
import me.twodee.friendlyneighbor.dto.UserLocationResult;
import me.twodee.friendlyneighbor.dto.UserLocationsResult;
import me.twodee.friendlyneighbor.entity.UserLocation;
import me.twodee.friendlyneighbor.service.Discovery;
import me.twodee.friendlyneighbor.service.Feed;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FnCoreHandlerTest
{
    private InProcessServer inProcessServer;
    private ManagedChannel channel;
    private FnCoreGrpc.FnCoreBlockingStub fnCoreHandler;

    @Mock
    Discovery discovery;

    @Mock
    Feed feed;

    /**
     * Creates an in-process server before each test
     */
    @BeforeEach
    void setUp() throws IllegalAccessException, IOException, InstantiationException
    {
        MockitoAnnotations.initMocks(this);
        inProcessServer = new InProcessServer();
        inProcessServer.start(new FnCoreHandler(discovery, feed));
        channel = InProcessChannelBuilder
                .forName("test")
                .directExecutor()
                .usePlaintext()
                .build();
        fnCoreHandler = FnCoreGrpc.newBlockingStub(channel);
    }

    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @AfterEach
    void cleanUp()
    {
        channel.shutdownNow();
        inProcessServer.stop();
    }

    FnCoreGenerated.SearchAreaRequest getRequest()
    {
        return FnCoreGenerated.SearchAreaRequest.newBuilder()
                .build();
    }

    /**
     * To test the server, make calls with a real stub using the in-process channel, and verify
     * behaviors or state changes from the client side.
     */
    @Test
    void testSuccessfulSaveUser()
    {
        when(discovery.saveUserLocation(any())).thenReturn(new ResultObject());

        FnCoreGenerated.RequestResult result = fnCoreHandler.saveUserLocation(
                FnCoreGenerated.RegistrationRequest.newBuilder().build());
        assertTrue(result.getSuccess());
    }

    @Test
    void testFailedSaveUser()
    {
        when(discovery.saveUserLocation(any())).thenReturn(
                new ResultObject("internal", ResultObject.SOMETHING_WENT_WRONG));

        FnCoreGenerated.RequestResult result = fnCoreHandler.saveUserLocation(
                FnCoreGenerated.RegistrationRequest.newBuilder().build());

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorsMap().containsKey("internal"));
        assertThat(result.getErrorsMap().get("internal"), equalTo(ResultObject.SOMETHING_WENT_WRONG));
    }

    @Test
    void testSuccessfulDeleteUser()
    {
        when(discovery.deleteUserLocation(anyString())).thenReturn(new ResultObject());

        FnCoreGenerated.RequestResult result = fnCoreHandler.deleteUserLocation(
                FnCoreGenerated.UserIdentifier.newBuilder().build());
        assertTrue(result.getSuccess());
    }

    @Test
    void testFailedDeleteUser()
    {
        when(discovery.deleteUserLocation(anyString())).thenReturn(
                new ResultObject("internal", ResultObject.SOMETHING_WENT_WRONG));

        FnCoreGenerated.RequestResult result = fnCoreHandler.deleteUserLocation(
                FnCoreGenerated.UserIdentifier.newBuilder().build());

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorsMap().containsKey("internal"));
        assertThat(result.getErrorsMap().get("internal"), equalTo(ResultObject.SOMETHING_WENT_WRONG));
    }

    @Test
    void testUserLookupWithLocation()
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation resultLoc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        resultLoc.setDistance(100);
        usersList.add(resultLoc);

        resultLoc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        resultLoc.setDistance(2.0);
        usersList.add(resultLoc);

        UserLocationsResult result = new UserLocationsResult(usersList);

        when(discovery.lookupNearbyUsersByLocation(any())).thenReturn(result);

        FnCoreGenerated.NearbyUsersResult locations = fnCoreHandler.findUsersInCircleByLocation(getRequest());
        assertTrue(locations.getMetaResult().getSuccess());
        Assertions.assertThat(locations.getUserList()).extracting("userId")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(locations.getUserList()).extracting("distance")
                .containsOnly(100.0, 2.0);
    }

    @Test
    void failedLookup()
    {
        Notification note = new Notification();
        note.addError("keyError", "valueErr");

        UserLocationsResult result = new UserLocationsResult(note);
        when(discovery.lookupNearbyUsersByLocation(any())).thenReturn(result);

        FnCoreGenerated.NearbyUsersResult locations = fnCoreHandler.findUsersInCircleByLocation(getRequest());

        assertFalse(locations.getMetaResult().getSuccess());
        assertThat(locations.getMetaResult().getErrorsMap().get("keyError"), equalTo("valueErr"));
    }

    @Test
    void testUserLookupWithId()
    {
        List<UserLocation> usersList = new ArrayList<>();

        UserLocation resultLoc = new UserLocation("abc123", new UserLocation.Position(10, 10), 10);
        resultLoc.setDistance(100);
        usersList.add(resultLoc);

        resultLoc = new UserLocation("hello", new UserLocation.Position(10, 10), 10);
        resultLoc.setDistance(2.0);
        usersList.add(resultLoc);

        UserLocationsResult result = new UserLocationsResult(usersList);

        when(discovery.lookupNearbyUsersByUserId(any())).thenReturn(result);

        FnCoreGenerated.UserIdentifier request = FnCoreGenerated.UserIdentifier.newBuilder().build();
        FnCoreGenerated.NearbyUsersResult locations = fnCoreHandler.findUsersInCircleById(request);
        assertTrue(locations.getMetaResult().getSuccess());
        Assertions.assertThat(locations.getUserList()).extracting("userId")
                .containsOnly("abc123", "hello");
        Assertions.assertThat(locations.getUserList()).extracting("distance")
                .containsOnly(100.0, 2.0);
    }

    @Test
    void failedLookupWithId()
    {
        Notification note = new Notification();
        note.addError("keyError", "valueErr");

        UserLocationsResult result = new UserLocationsResult(note);
        when(discovery.lookupNearbyUsersByUserId(any())).thenReturn(result);

        FnCoreGenerated.UserIdentifier request = FnCoreGenerated.UserIdentifier.newBuilder().build();
        FnCoreGenerated.NearbyUsersResult locations = fnCoreHandler.findUsersInCircleById(request);

        assertFalse(locations.getMetaResult().getSuccess());
        assertThat(locations.getMetaResult().getErrorsMap().get("keyError"), equalTo("valueErr"));
    }

    @Test
    void successfulUserLocationFetch()
    {
        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder()
                .setUserId("abc")
                .build();
        when(discovery.getUserLocation(any())).thenReturn(new UserLocationResult(
                new UserLocation("abc", new UserLocation.Position(22.3, 77.0), 10)
        ));

        FnCoreGenerated.LocationRadiusResult result = fnCoreHandler.getUserLocation(identifier);

        assertTrue(result.getMetaResult().getSuccess());
        assertThat(result.getLocation().getLatitude(), equalTo(22.3));
        assertThat(result.getLocation().getLongitude(), equalTo(77.0));
        assertThat(result.getRadius(), equalTo(10.0));
    }

    @Test
    void invalidUserLocationFetch()
    {
        FnCoreGenerated.UserIdentifier identifier = FnCoreGenerated.UserIdentifier.newBuilder()
                .setUserId("abc")
                .build();
        UserLocationResult userLocationResult = new UserLocationResult();
        Notification note = new Notification();
        note.addError("id", "err");
        userLocationResult.setNotification(note);
        when(discovery.getUserLocation(any())).thenReturn(userLocationResult);

        FnCoreGenerated.LocationRadiusResult result = fnCoreHandler.getUserLocation(identifier);

        assertFalse(result.getMetaResult().getSuccess());
        assertTrue(result.getMetaResult().containsErrors("id"));
    }

    public void shutdown() throws InterruptedException
    {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}