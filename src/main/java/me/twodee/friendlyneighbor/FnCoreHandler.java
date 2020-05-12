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
    public void registerUser(FnCoreGenerated.RegistrationRequest request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {
        responseObserver.onNext(saveNewUser(request));
        responseObserver.onCompleted();
    }

    private FnCoreGenerated.RequestResult saveNewUser(FnCoreGenerated.RegistrationRequest request)
    {
       discovery.registerUser(request);
       return FnCoreGenerated.RequestResult.newBuilder().setSuccess(true).build();
    }

    @Override
    public void deleteUser(FnCoreGenerated.UserIdentifier request, StreamObserver<FnCoreGenerated.RequestResult> responseObserver)
    {

    }

    @Override
    public void findUsersInCircle(FnCoreGenerated.SearchAreaRequest request, StreamObserver<FnCoreGenerated.UserInVicinity> responseObserver)
    {
        super.findUsersInCircle(request, responseObserver);
    }


}
