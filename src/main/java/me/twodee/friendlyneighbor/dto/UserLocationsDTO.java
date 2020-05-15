package me.twodee.friendlyneighbor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.twodee.friendlyneighbor.entity.UserLocation;

import java.util.List;

@AllArgsConstructor
@Getter
public class UserLocationsDTO extends DataTransferObject
{
    private List<UserLocation> userLocations;

    public UserLocationsDTO(Notification notification)
    {
        setNotification(notification);
    }
}
