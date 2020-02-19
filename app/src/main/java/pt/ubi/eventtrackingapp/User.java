package pt.ubi.eventtrackingapp;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

class User implements Parcelable {
    private String email;
    private String password;
    private String username;
    private String user_id;
    private String mImageUrl;

    User() {

    }

    User(String email, String username) {
        this.email =  email;
        this.username = username;
    }

    User(String email, String username, String user_id, String mImageUrl) {
        this.email =  email;
        this.username = username;
        this.user_id = user_id;
        this.mImageUrl = mImageUrl;
    }

    protected User(Parcel in) {
        email = in.readString();
        password = in.readString();
        username = in.readString();
        user_id = in.readString();
        mImageUrl = in.readString();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(password);
        dest.writeString(username);
        dest.writeString(user_id);
        dest.writeString(mImageUrl);

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getmImageUrl() {
        return mImageUrl;
    }

    public void setmImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }
}
