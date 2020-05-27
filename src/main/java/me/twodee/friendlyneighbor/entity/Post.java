package me.twodee.friendlyneighbor.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Document
public class Post {
    @Id
    private String id;
    private UserLocation location;
    private LocalDateTime time;
    private PostType type;
    @TextIndexed(weight = 2)
    private String title;

    @Transient
    @TextScore
    Float score;

    public enum PostType {
        REQUEST,
        OFFERING
    }

    public Post(String id, UserLocation location, LocalDateTime time) {
        this.id = id;
        this.location = location;
        this.time = time;
    }

    public Post(String id, UserLocation location, LocalDateTime time, PostType type, String title) {
        this.id = id;
        this.location = location;
        this.time = time;
        this.type = type;
        this.title = title;
    }
}