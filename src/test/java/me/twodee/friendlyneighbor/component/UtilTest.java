package me.twodee.friendlyneighbor.component;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

class UtilTest
{

    @Test
    void testHaversineCorrectness()
    {
        assertThat(Math.abs(Util.haversine(19.416575, 72.807543, 22.623806, 88.414486) - (1657.79)),
                   lessThanOrEqualTo(1.0));
    }
}