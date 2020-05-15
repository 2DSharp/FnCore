package me.twodee.friendlyneighbor;

import io.grpc.stub.StreamObserver;
import me.twodee.friendlyneighbor.service.Discovery;

import javax.inject.Inject;

public class FnCoreHandler extends FnCoreGrpc.FnCoreImplBase
{
    private final Discovery discovery;

    @Inject
    public FnCoreHandler(Discovery discovery)
    {
        this.discovery = discovery;
    }

    @Override
    public void saveUserLocation(FnCoreGenerated.RegistrationRequest request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {
        responseObserver.onNext(discovery.registerUser(request));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteUserLocation(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {
        responseObserver.onNext(discovery.deleteUser(request));
        responseObserver.onCompleted();
    }

    @Override
    public void findUsersInCircleByLocation(FnCoreGenerated.SearchAreaRequest request, StreamObserver<FnCoreGenerated.NearbyUsersResult> responseObserver)
    {
        responseObserver.onNext(discovery.lookupNearbyUsersByLocation(request));
        responseObserver.onCompleted();
    }

    @Override
    public void findUsersInCircleById(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.NearbyUsersResult> responseObserver)
    {
        responseObserver.onNext(discovery.lookupNearbyUsersByUserId(request));
        responseObserver.onCompleted();
    }
}
