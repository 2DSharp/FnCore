package me.twodee.friendlyneighbor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import me.twodee.friendlyneighbor.component.FnCoreConfig;
import me.twodee.friendlyneighbor.configuration.LocationModule;
import me.twodee.friendlyneighbor.configuration.NotifierModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
public class Main
{
    public static void main(String[] args)
    {
        try {
            Properties properties = new Properties();
            if (args.length > 0) {
                // The user's specified file takes top priority
                Path path = Paths.get(args[0]);
                if (Files.exists(path)) {
                    System.out.println(
                            "Found configuration file! Reading from " + path.getFileName().toAbsolutePath().toString());
                    log.info("Found configuration file! Reading from " + path.getFileName().toAbsolutePath().toString());
                    properties.load(Files.newInputStream(path));
                }
            } else {
                Path path = Paths.get("fnconfig.properties");
                // Does a fnconfig already exist? Load it
                if (Files.exists(path)) {
                    System.out.println("Found configuration file! Reading from " + path.getFileName().toString());
                    log.info("Found configuration file! Reading from " + path.getFileName().toAbsolutePath().toString());
                    properties.load(Files.newInputStream(path));
                }
            }
            // If user didn't specify a file and there's no fnconfig, continue with empty props and read defaults
            FnCoreConfig config = FnCoreConfig.createFromProperties(properties);
            assert config != null;
            Injector injector = Guice.createInjector(new LocationModule(config), new NotifierModule(config));
            FnCoreHandler service = injector.getInstance(FnCoreHandler.class);
            Server server = new Server(config.getFnCorePort(), service);
            server.start();
        } catch (Throwable e) {
            log.error("Something broke while starting: " + e.getMessage(),  e);
        }
    }
}