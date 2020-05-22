package me.twodee.friendlyneighbor.entity;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class UserLocationTest
{

    @Test
    void getPosition()
    {
        UserLocation.Position position = new UserLocation.Position(23.312, 0.2);
        assertThat(position.getLatitude(), equalTo(23.312));
        assertThat(position.getLongitude(), equalTo(0.2));
    }

    @Test
    void setCorrectLocation()
    {
        UserLocation res = new UserLocation("id", new UserLocation.Position(23.312, 0.2), 0.1);
        assertThat(res.getPosition().getLatitude(), equalTo(23.312));
        assertThat(res.getPosition().getLongitude(), equalTo(0.2));
        assertThat(res.getId(), equalTo("id"));
        assertThat(res.getRadius(), equalTo(0.1));
    }
}