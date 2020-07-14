package pt.ubi.eventtrackingapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

public class CustomGeoPoint extends GeoPoint implements Parcelable {

    public CustomGeoPoint(double latitude, double longitude) {
        super(latitude, longitude);
    }

    protected CustomGeoPoint(Parcel in) {
        super( in.readDouble(), in.readDouble());

    }

    public static final Creator<CustomGeoPoint> CREATOR = new Creator<CustomGeoPoint>() {
        @Override
        public CustomGeoPoint createFromParcel(Parcel in) {
            return new CustomGeoPoint(in);
        }

        @Override
        public CustomGeoPoint[] newArray(int size) {
            return new CustomGeoPoint[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(getLatitude());
        dest.writeDouble(getLongitude());
    }
}
