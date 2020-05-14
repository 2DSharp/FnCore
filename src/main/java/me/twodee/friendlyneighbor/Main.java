package me.twodee.friendlyneighbor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.twodee.friendlyneighbor.configuration.LocationModule;

import java.io.IOException;
import java.util.Properties;


public class Main
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        Injector injector = Guice.createInjector(new LocationModule());
        FnCoreHandler service = injector.getInstance(FnCoreHandler.class);
        Properties properties = new Properties();
        properties.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));

        Server server = new Server(Integer.parseInt(properties.getProperty("server.port")), service);
        server.start();
    }
}
