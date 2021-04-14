package edu.ntnu.app.psta;

public class Location {

    double latitude;
    double longitude;
    String name;
    String city;
    String country;

    public Location(double latitude, double longitude, String city, String country) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.country = country;
    }

    public int getId() {
        return 0;
    }
}
