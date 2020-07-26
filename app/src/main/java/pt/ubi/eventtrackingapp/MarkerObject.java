package pt.ubi.eventtrackingapp;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class MarkerObject {

    private CustomGeoPoint geoPoint;
    private @ServerTimestamp
    Date timestamp;
    private String User_id;
    private String imageUrl;
    private String eventId;
    private String description;
    private String imageName;


    public MarkerObject(CustomGeoPoint geoPoint, String user_id, String imageUrl, String eventId, String description, String imageName) {
        this.geoPoint = geoPoint;
        this.User_id = user_id;
        this.imageUrl = imageUrl;
        this.eventId = eventId;
        this.description = description;
        this.imageName = imageName;

    }

    public MarkerObject() {

    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUser_id() {
        return User_id;
    }

    public void setUser_id(String user_id) {
        User_id = user_id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public CustomGeoPoint getGeoPoint() {
        return geoPoint;
    }

    private void setGeoPoint(CustomGeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
