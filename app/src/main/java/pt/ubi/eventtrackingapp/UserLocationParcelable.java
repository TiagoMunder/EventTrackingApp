package pt.ubi.eventtrackingapp;

import android.os.Parcel;
import android.os.Parcelable;


import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class UserLocationParcelable implements Parcelable {
    private CustomGeoPoint geoPoint;
    private @ServerTimestamp
    Date timestamp;
    private User user;

    public UserLocationParcelable(CustomGeoPoint geoPoint, Date timestamp, User user) {
        this.user = user;
        this.geoPoint = geoPoint;
        this.timestamp = timestamp;
    }


    public UserLocationParcelable() {
    }

    protected UserLocationParcelable(Parcel in) {
        user = (User)in.readValue(User.class.getClassLoader());
        geoPoint = (CustomGeoPoint)in.readValue(CustomGeoPoint.class.getClassLoader());
        timestamp = (Date)in.readValue(Date.class.getClassLoader());
    }

    public static final Creator<UserLocationParcelable> CREATOR = new Creator<UserLocationParcelable>() {
        @Override
        public UserLocationParcelable createFromParcel(Parcel in) {
            return new UserLocationParcelable(in);
        }

        @Override
        public UserLocationParcelable[] newArray(int size) {
            return new UserLocationParcelable[size];
        }
    };

    public CustomGeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(CustomGeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "UserLocation{" +
                "geoPoint=" + geoPoint +
                ", timestamp='" + timestamp + '\'' +
                ", user=" + user +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(user);
        dest.writeValue(geoPoint);
        dest.writeValue(timestamp);
    }
}
