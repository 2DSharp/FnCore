package me.twodee.friendlyneighbor.component;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FnCoreConfigTest
{

    @Test
    void createFromEmptyProperties()
    {
        FnCoreConfig config = FnCoreConfig.createFromProperties(new Properties());
        assertNotNull(config);
        assertThat(config.getFnCorePort(), equalTo(9120));
    }

    @Test
    void createFromLoadedProperties()
    {
        Properties properties = new Properties();
        properties.setProperty("redis.port", "1000");
        properties.setProperty("server.port", "10");
        FnCoreConfig config = FnCoreConfig.createFromProperties(properties);

        assertNotNull(config);
        assertThat(config.getFnCorePort(), equalTo(10));
        assertThat(config.getRedisPort(), equalTo(1000));
    }

    @Test
    void createFromInvalidPropertiesRestoresFromDefault()
    {
        Properties properties = new Properties();
        properties.setProperty("redis.port", "j");
        properties.setProperty("server.port", "10");
        FnCoreConfig config = FnCoreConfig.createFromProperties(properties);

        assertNotNull(config);
        assertThat(config.getFnCorePort(), equalTo(9120));
        assertThat(config.getRedisPort(), equalTo(6379));
    }
}