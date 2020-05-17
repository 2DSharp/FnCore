package me.twodee.friendlyneighbor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.twodee.friendlyneighbor.entity.UserLocation;

import java.util.List;

@AllArgsConstructor
@Getter
public class UserLocationsResult extends ResultObject
{
    private List<UserLocation> userLocations;

    public UserLocationsResult(Notification notification)
    {
        setNotification(notification);
    }
}
