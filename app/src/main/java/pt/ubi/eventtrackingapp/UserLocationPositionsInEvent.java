package pt.ubi.eventtrackingapp;

import com.google.firebase.firestore.GeoPoint;

public class UserLocationPositionsInEvent {

    private String userPositionKey;
    private float distanceTraveled;
    private GeoPoint lastPosition;
    private float velocity;
    private int duration;


    public UserLocationPositionsInEvent( float distanceDone, GeoPoint lastPosition, float velocity, int duration) {

        this.distanceTraveled = distanceDone;
        this.lastPosition = lastPosition;
        this.velocity = velocity;
        this.duration = duration;

    }

    public float getDistanceTraveled() {
        return distanceTraveled;
    }

    public void setDistanceTraveled( float distance) {
         this.distanceTraveled = distance;
    }


    public String getUserPositionKey() {
        return userPositionKey;
    }

    public void setUserPositionKey(String userPositionKey) {
        this.userPositionKey = userPositionKey;
    }

    public GeoPoint getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(GeoPoint lastPosition) {
        this.lastPosition = lastPosition;
    }

    public float getVelocity() {
        return velocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
