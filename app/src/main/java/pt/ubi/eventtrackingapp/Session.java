package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

public class Session {
    private SharedPreferences prefs;

    public Session(Context cntx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(cntx);
    }
    public void setUsername(String username) {
        prefs.edit().putString("USERNAME", username).apply();
    }
    public String getUsername() {
        String username = prefs.getString("USERNAME","");
        return username;
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

}
