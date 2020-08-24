package pt.ubi.eventtrackingapp;

import com.google.firebase.firestore.GeoPoint;

public class UserPosition {
    private GeoPoint geoPoint;
    private String time;

    public UserPosition(){}

    public UserPosition(GeoPoint geoPoint, String time) {
        this.geoPoint = geoPoint;
        this.time = time;
    }

    public UserPosition(double lat, double longitude, String time) {
        this.geoPoint = new GeoPoint(lat, longitude);
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }
}
