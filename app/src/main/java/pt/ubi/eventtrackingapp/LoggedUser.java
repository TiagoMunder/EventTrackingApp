package pt.ubi.eventtrackingapp;

import android.app.Application;

public class LoggedUser extends Application {
    private User user = null;

    public User getUser() { return user;}

    public void setUser(User user){
        this.user = user;
    }
}
