package me.twodee.friendlyneighbor.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@Setter
@NoArgsConstructor
@Document(collection = "location")
public class UserLocation
{
    @Setter
    @Getter
    @AllArgsConstructor
    @ToString
    public static class Position
    {
        private double latitude;
        private double longitude;
    }

    @Id
    private String id;
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] position;
    private Number distance;
    private double radius;

    public UserLocation(String id, Position position, double radius)
    {
        this.id = id;
        this.position = new double[]{ position.getLongitude(), position.getLatitude() };
        this.radius = radius;
    }

    public Position getPosition()
    {
        return new Position(position[1], position[0]);
    }
}
