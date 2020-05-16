package me.twodee.friendlyneighbor.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class Post
{
    @Id
    private String id;
    @Transient
    private UserLocation location;
    private LocalDateTime time;
}