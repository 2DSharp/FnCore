package me.twodee.friendlyneighbor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.twodee.friendlyneighbor.configuration.LocationModule;

import java.io.IOException;


public class Main
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        Injector injector = Guice.createInjector(new LocationModule());
        FnCoreHandler service = injector.getInstance(FnCoreHandler.class);
        Server server = new Server(9120, service);
        server.start();
    }
}
