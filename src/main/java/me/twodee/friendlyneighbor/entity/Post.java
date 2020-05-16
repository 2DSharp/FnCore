package me.twodee.friendlyneighbor.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class Post
{
    private String id;
    private UserLocation location;
}