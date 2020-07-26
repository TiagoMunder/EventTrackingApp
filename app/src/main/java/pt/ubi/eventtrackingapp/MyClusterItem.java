package pt.ubi.eventtrackingapp;


import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


public class MyClusterItem implements ClusterItem {

    private LatLng position; // required field
    private String title; // required field
    private String snippet; // required field
    private String iconPicture;
    private User user;
    private Object mTag;

    public MyClusterItem(LatLng position, String title, String snippet, String iconPicture, User user) {
        this.position = position;
        this.title = title;
        this.snippet = snippet;
        this.iconPicture = iconPicture;
        this.user = user;
        this.mTag = MyClusterItem.class;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(final Object tag) {
        mTag = tag;
    }

    public MyClusterItem() {
        this.mTag = MyClusterItem.class;
    }

    public String getIconPicture() {
        return iconPicture;
    }

    public void setIconPicture(String iconPicture) {
        this.iconPicture = iconPicture;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public LatLng getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }
}