package me.twodee.friendlyneighbor.component;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.Properties;

@Getter
@Builder
@Log
public class FnCoreConfig
{
    private final long feedCacheExpiry;
    private final String mongoConnectionString;
    private final String mongoDatabase;
    private final String redisKeyspace;
    private final String redisHostName;
    private final int redisPort;
    private final int fnCorePort;

    // TODO: Add redis password support
    public static FnCoreConfig createFromProperties(Properties properties)
    {
        try {
            Properties defaultProps = new Properties();
            defaultProps.load(FnCoreConfig.class.getClassLoader().getResourceAsStream("config.properties"));

            return FnCoreConfig.builder()
                    .feedCacheExpiry(Long.parseLong(
                            properties.getProperty("feed.expiry", defaultProps.getProperty("feed.expiry"))))
                    .fnCorePort(Integer.parseInt(
                            properties.getProperty("server.port", defaultProps.getProperty("server.port", "9120"))))
                    .redisPort(Integer.parseInt(
                            properties.getProperty("redis.port", defaultProps.getProperty("redis.port"))))
                    .mongoConnectionString(properties.getProperty("mongo.connection_string",
                                                                  defaultProps.getProperty("mongo.connection_string")))
                    .redisHostName(properties.getProperty("redis.hostname",
                                                          defaultProps.getProperty("redis.hostname", "localhost")))
                    .mongoDatabase(
                            properties.getProperty("mongo.database", defaultProps.getProperty("mongo.database", "")))
                    .redisKeyspace(properties.getProperty("redis.keyspace",
                                                          defaultProps.getProperty("redis.keyspace", "FNCORE")))
                    .build();


        } catch (IOException e) {
            log.severe("Reading default properties failed. Something went seriously wrong. " +
                               "Did you remove the configuration.properties before compiling?");
            return null;
        }

    }
}
