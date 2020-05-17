package me.twodee.friendlyneighbor.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.twodee.friendlyneighbor.entity.UserLocation;

@NoArgsConstructor
@AllArgsConstructor
public class UserLocationResult extends ResultObject
{
    public UserLocation userLocation;
}
