package pt.ubi.eventtrackingapp;

import java.util.Date;

public class Event {
    private String owner, eventID;
    private String name;
    private String description;
    private String street, city, country, date;

    public Event() {}

    public Event(String owner, String name, String description) {
        this.owner = owner;
        this.name = name;
        this.description = description;
    }

    public Event(String owner, String name, String description, String eventID) {
        this.eventID =  eventID;
        this.owner = owner;
        this.name = name;
        this.description = description;
    }

    public Event(String owner, String name, String description, String street, String city, String country, String date, String eventID) {
        this.eventID = eventID;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.street = street;
        this.city = city;
        this.country = country;
        this.date = date;
    }
    public Event(String owner, String name, String description, String street, String city, String country, String date) {
        this.eventID = eventID;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.street = street;
        this.city = city;
        this.country = country;
        this.date = date;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }
}
