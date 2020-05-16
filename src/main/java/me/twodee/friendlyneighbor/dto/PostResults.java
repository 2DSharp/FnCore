package me.twodee.friendlyneighbor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.twodee.friendlyneighbor.entity.Post;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostResults extends ResultObject
{
    private List<Post> posts;
}
