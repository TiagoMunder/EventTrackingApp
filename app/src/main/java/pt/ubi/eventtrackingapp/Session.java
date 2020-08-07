package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import static pt.ubi.eventtrackingapp.Constants.CURRENTLOCATION;

public class Session {
    private SharedPreferences prefs;

    public Session(Context cntx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(cntx);
    }

    public void setUserInfo(User user) {
        Gson gson = new Gson();
        String jsonUser = gson.toJson(user);
        prefs.edit().putString("USERINFO", jsonUser).apply();
    }

    public String getUsername() {
        String userInfo = prefs.getString("USERINFO","");
        Gson gson = new Gson();
        User obj = gson.fromJson(userInfo, User.class);
        return obj.getUsername();
    }

    public User getUser() {
        String userInfo = prefs.getString("USERINFO","");
        Gson gson = new Gson();
        User obj = gson.fromJson(userInfo, User.class);
        return obj;
    }

    public Location getCurrentLocation() {
        String currentlocation = prefs.getString(CURRENTLOCATION,"");
        Gson gson = new Gson();
        Location obj = gson.fromJson(currentlocation, Location.class);
        return obj;
    }

    public void setCurrentLocation(Location currentLocation) {
        Gson gson = new Gson();
        String jsonCurrentLocation = gson.toJson(currentLocation);
        prefs.edit().putString(CURRENTLOCATION, jsonCurrentLocation).apply();
    }

    public void setEvent(Event event) {

        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(event);
        prefsEditor.putString("EVENTINFO", json);
        prefsEditor.commit();
    }

    public Event getEvent() {
        Gson gson = new Gson();
        String json = prefs.getString("EVENTINFO", "");
        Event event = gson.fromJson(json, Event.class);
        return event;
    }

    public void reset() {
        prefs.edit().clear().commit();
    }

}
