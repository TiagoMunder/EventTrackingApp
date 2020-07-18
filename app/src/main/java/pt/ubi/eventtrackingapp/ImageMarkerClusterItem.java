package pt.ubi.eventtrackingapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ImageMarkerClusterItem implements ClusterItem {
    private LatLng position; // required field
    private String title; // required field (this will be image Name)
    private String snippet; // required field
    private String iconPicture;
    private String User_id;
    private String eventId;
    private String description;

    public ImageMarkerClusterItem(LatLng position, String title, String snippet, String iconPicture, String eventId, String User_id, String description) {
        this.position = position;
        this.title = title;
        this.snippet = snippet;
        this.iconPicture = iconPicture;
        this.User_id = User_id;
        this.eventId = eventId;
        this.description = description;
    }

    public ImageMarkerClusterItem() {

    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    public String getIconPicture() {
        return iconPicture;
    }

    public void setIconPicture(String iconPicture) {
        this.iconPicture = iconPicture;
    }

    public String getUser_id() {
        return User_id;
    }

    public void setUser_id(String user_id) {
        User_id = user_id;
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

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
