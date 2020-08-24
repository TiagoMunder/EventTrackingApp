package pt.ubi.eventtrackingapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class MarkerObject  implements Parcelable {

    private CustomGeoPoint geoPoint;
    private @ServerTimestamp
    Date timestamp;
    private String User_id;
    private String imageUrl;
    private String eventId;
    private String description;
    private String imageName;
    private String id;


    public MarkerObject(CustomGeoPoint geoPoint, String user_id, String imageUrl, String eventId, String description, String imageName, String id) {
        this.geoPoint = geoPoint;
        this.User_id = user_id;
        this.imageUrl = imageUrl;
        this.eventId = eventId;
        this.description = description;
        this.imageName = imageName;
        this.id = id;

    }

    public MarkerObject() {

    }

    protected MarkerObject(Parcel in) {
        geoPoint = in.readParcelable(CustomGeoPoint.class.getClassLoader());
        User_id = in.readString();
        imageUrl = in.readString();
        eventId = in.readString();
        description = in.readString();
        imageName = in.readString();
        id = in.readString();
    }

    public static final Creator<MarkerObject> CREATOR = new Creator<MarkerObject>() {
        @Override
        public MarkerObject createFromParcel(Parcel in) {
            return new MarkerObject(in);
        }

        @Override
        public MarkerObject[] newArray(int size) {
            return new MarkerObject[size];
        }
    };

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(geoPoint, flags);
        dest.writeString(User_id);
        dest.writeString(imageUrl);
        dest.writeString(eventId);
        dest.writeString(description);
        dest.writeString(imageName);
        dest.writeString(id);
    }
}
