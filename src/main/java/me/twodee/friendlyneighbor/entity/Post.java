package me.twodee.friendlyneighbor.entity;

import lombok.*;
import org.springframework.data.annotation.Id;

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
    private UserLocation location;
    private LocalDateTime time;
}