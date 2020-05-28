package me.twodee.friendlyneighbor.dto;

import lombok.*;
import me.twodee.friendlyneighbor.entity.Post;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PostResults extends ResultObject
{
    private List<Post> posts;
}
