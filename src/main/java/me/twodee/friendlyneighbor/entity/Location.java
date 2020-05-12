package me.twodee.friendlyneighbor.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection="location")
public class Location
{
    @Id
    private String id;
    private double[] position;
    private Number dis;
    private double radius;

    public Location(String id, double[] position, double radius)
    {
        this.id = id;
        this.position = position;
        this.radius = radius;
    }
}
