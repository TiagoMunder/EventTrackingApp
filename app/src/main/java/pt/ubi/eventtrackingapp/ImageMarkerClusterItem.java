package pt.ubi.eventtrackingapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ImageMarkerClusterItem implements ClusterItem, Parcelable {
    private LatLng position; // required field
    private String title; // required field (this will be image Name)
    private String snippet; // required field
    private String iconPicture;
    private String User_id;
    private String eventId;
    private String description;
    private Object mTag;
    private String id;

    public ImageMarkerClusterItem(LatLng position, String title, String snippet, String iconPicture, String eventId, String User_id, String description, String id) {
        this.position = position;
        this.title = title;
        this.snippet = snippet;
        this.iconPicture = iconPicture;
        this.eventId = eventId;
        this.User_id = User_id;
        this.description = description;
        this.mTag = ImageMarkerClusterItem.class;
        this.id = id;
    }

    public ImageMarkerClusterItem() {
        this.mTag = ImageMarkerClusterItem.class;
    }


    protected ImageMarkerClusterItem(Parcel in) {
        position = in.readParcelable(LatLng.class.getClassLoader());
        title = in.readString();
        snippet = in.readString();
        iconPicture = in.readString();
        User_id = in.readString();
        eventId = in.readString();
        description = in.readString();
        mTag = ImageMarkerClusterItem.class;
        id = in.readString();
    }

    public static final Creator<ImageMarkerClusterItem> CREATOR = new Creator<ImageMarkerClusterItem>() {
        @Override
        public ImageMarkerClusterItem createFromParcel(Parcel in) {
            return new ImageMarkerClusterItem(in);
        }

        @Override
        public ImageMarkerClusterItem[] newArray(int size) {
            return new ImageMarkerClusterItem[size];
        }
    };

    @Override
    public LatLng getPosition() {
        return position;
    }

    public void setTag(final Object tag) {
        mTag = tag;
    }
    public Object getTag() {
        return mTag;
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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(position);
        dest.writeValue(title);
        dest.writeValue(snippet);
        dest.writeValue(iconPicture);
        dest.writeValue(User_id);
        dest.writeValue(eventId);
        dest.writeValue(description);
        dest.writeValue(mTag);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
